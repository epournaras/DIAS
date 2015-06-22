/*
 * Copyright (C) 2015 Evangelos Pournaras
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package protocols;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import peerlets.DIAS;
import peerlets.PeerSamplingService;
import protopeer.Experiment;
import protopeer.NeighborManager;
import protopeer.Peer;
import protopeer.PeerFactory;
import protopeer.SimulatedExperiment;
import protopeer.servers.bootstrap.BootstrapClient;
import protopeer.servers.bootstrap.BootstrapServerUniform;
import protopeer.servers.bootstrap.SimpleConnector;
import protopeer.servers.bootstrap.SimplePeerIdentifierGenerator;
import protopeer.util.quantities.Time;
import bloomfilter.CHashFactory;

import communication.AggregationStrategy;

import consistency.BloomFilterParams;
import consistency.BloomFilterType;
import dsutil.protopeer.services.aggregation.AggregationType;
import enums.PeerSelectionPolicy;
import enums.ViewPropagationPolicy;

/**
 *
 * @author Evangelos
 */
public class DIASApplExperiment extends SimulatedExperiment
{

    private final static String expSeqNum="01";
    private final static String expID="dump/Experiment"+expSeqNum+"/";

    //Simulation Parameters
    private final static int runDuration=300;
    private final static int N=3;

    //Peer Sampling Service
    /*  Each node maintains addresses by means of a partial view, which is a set of c node descriptors. */
    private final static int c=4;
    private final static int H=0; //healing. force remove some neihgbors periodically to elimiate dead links
    private final static int S=2; //swap. should be min. c/2
    private final static ViewPropagationPolicy viewPropagationPolicy=ViewPropagationPolicy.PUSHPULL;
    private final static PeerSelectionPolicy peerSelectionPolicy=PeerSelectionPolicy.RAND;
    private final static int Tpss=250;
    private final static int A=1000;
    private final static int B=6000;
    
    //DIAS Service Parameterization
    private final static int Tdias=1000;
    private final static int Tsampling=250;
    private final static int sampleSize=2;
    private final static int numOfSessions=10;
    private final static int unexploitedSize=2;
    private final static int outdatedSize=2;
    private final static int exploitedSize=2;
    private final static AggregationStrategy.Strategy strategy=AggregationStrategy.Strategy.EXPLOITATION;
    private final static BloomFilterType amsType=BloomFilterType.COUNTING;
    private final static int amsHashType=CHashFactory.DOUBLE_HASH;
    private final static int ams_m=16;
    private final static int ams_k=24;
    private final static int dmaHashType=CHashFactory.DOUBLE_HASH;
    private final static int dma_m=16;
    private final static int dma_k=24;
    private final static int amdHashType=CHashFactory.DOUBLE_HASH;
    private final static int amd_m=16;
    private final static int amd_k=24;
    private final static int smaHashType=CHashFactory.DOUBLE_HASH;
    private final static int sma_m=16;
    private final static int sma_k=24;
    private final static Map<BloomFilterParams, Object> bfParams=new HashMap<BloomFilterParams, Object>();
    
    //DIAS Application Parameterization
    private final static AggregationType type=AggregationType.ARITHMETIC;
    private final static int Tboot=15000;
    private final static int Taggr=runDuration*1000;
    private final static int k=5;
    private final static double minValueDomain=0;
    private final static double maxValueDomain=1;
    private final static double Pt=1.0;
    private final static double Ps=1.0;
    private final static int t=10000;
    private final static GenerationScheme genScheme=GenerationScheme.BETA;
    private final static SelectionScheme selScheme=SelectionScheme.CYCLICAL;


//     @Override
//	public NetworkInterfaceFactory createNetworkInterfaceFactory() {
//		return new DelayLossNetworkInterfaceFactory(getEventScheduler(),new UniformDelayModel(0.15,2.5));
//	}

    public static void main(String[] args) {
        System.out.println(expID+"...\n");
        Experiment.initEnvironment();
        final DIASApplExperiment dias = new DIASApplExperiment();
        dias.init();
        final File folder = new File("dump/"+expID);
        folder.mkdirs();
        PeerFactory peerFactory=new PeerFactory() {
            public Peer createPeer(int peerIndex, Experiment experiment) {
                Peer newPeer = new Peer(peerIndex);
                if (peerIndex == 0) {
                	//CallsRingTopologyGenerator, which requires initialNodeDegree to set how many neighbors a client will receive, and initialCoreNodes to know how long to wait.
                    newPeer.addPeerlet(new BootstrapServerUniform());
                }
                //list. stores neighbors
                newPeer.addPeerlet(new NeighborManager());
                //gets (init) neighbors from BootstrapClient, then connects to them. sucessful candidates are stored.
                newPeer.addPeerlet(new SimpleConnector());
                //sends ID to BootstrapServer + gets init neighbors, then stops
                newPeer.addPeerlet(new BootstrapClient(Experiment.getSingleton().getAddressToBindTo(0), new SimplePeerIdentifierGenerator()));
                // Update of Neighbors, smarter than SimpleConnector
                newPeer.addPeerlet(new PeerSamplingService(c, H, S, peerSelectionPolicy, viewPropagationPolicy, Tpss, A, B));
                //CoreAggregator
                newPeer.addPeerlet(new DIAS(expID, Tdias, numOfSessions, Tsampling, sampleSize, strategy, unexploitedSize, outdatedSize, exploitedSize, collectBloomFilterParams()));
                //Would sit on top: like a fake sensor and data collector. Can be replaced e.g. with PlanetaryNervosSensors later.
                newPeer.addPeerlet(new SimpleDIASApplication(expID, Tboot, Taggr, k, minValueDomain, maxValueDomain, t, Pt, Ps, genScheme, selScheme, type));
                return newPeer;
            }
        };
        
        dias.initPeers(0,N,peerFactory);
        dias.startPeers(0,N);

        //run the simulation
        dias.runSimulation(Time.inSeconds(runDuration));  
        /* runSimulation => EventScheduler.run == while(!timeout) processNextEvent();
         * where do the "events" come from?

         * case Exeriment = SimulatedExperiment:
         *   SimulatedExperiment.createClock => new SimulatedClock(eventscheduler) => new SimulatedTimer
         *   DIAS.schedule => SimulatedTimer.schedule => EvendScheduler.enqueueEvent == events.add(event)
         * case Experiment = LieveExperiment:
         *   LiveExperiment.createClock => new RealClock() == new RealTimer
         *   no EventScheduler() available, thus no runSimulation() available
         */

        

        //AETOSLogReplayer replayer=new AETOSLogReplayer("peersLog/"+folder.getName()+"/", 0, 50);
    }

    private static Map<BloomFilterParams, Object> collectBloomFilterParams(){
        bfParams.put(BloomFilterParams.AMS_TYPE, amsType);
        bfParams.put(BloomFilterParams.AMS_HASH_TYPE, amsHashType);
        bfParams.put(BloomFilterParams.AMS_M, ams_m);
        bfParams.put(BloomFilterParams.AMS_K, ams_k);
        bfParams.put(BloomFilterParams.AMD_HASH_TYPE, amdHashType);
        bfParams.put(BloomFilterParams.AMD_M, amd_m);
        bfParams.put(BloomFilterParams.AMD_K, amd_k);
        bfParams.put(BloomFilterParams.DMA_HASH_TYPE, dmaHashType);
        bfParams.put(BloomFilterParams.DMA_M, dma_m);
        bfParams.put(BloomFilterParams.DMA_K, dma_k);
        bfParams.put(BloomFilterParams.SMA_HASH_TYPE, smaHashType);
        bfParams.put(BloomFilterParams.SMA_M, sma_m);
        bfParams.put(BloomFilterParams.SMA_K, sma_k);
        return bfParams;
    }


}
