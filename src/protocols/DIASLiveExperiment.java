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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import peerlets.DIAS;
import peerlets.PeerSamplingService;
import protopeer.Experiment;
import protopeer.LiveExperiment;
import protopeer.MainConfiguration;
import protopeer.NeighborManager;
import protopeer.Peer;
import protopeer.PeerFactory;
import protopeer.servers.bootstrap.BootstrapClient;
import protopeer.servers.bootstrap.BootstrapServerUniform;
import protopeer.servers.bootstrap.SimpleConnector;
import protopeer.servers.bootstrap.SimplePeerIdentifierGenerator;
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
public class DIASLiveExperiment extends LiveExperiment {

    private static String expID="LiveExperiment/";

	// Simulation Parameters
	//private final static int runDuration = 500;
	//private final static int N = 500;

	// Peer Sampling Service
	private final static int c = 10; //view (max number of neighbors)
	private final static int H = 0; 
	private final static int S = 4; //PeersSamplingSErvice paper formula
	private final static ViewPropagationPolicy viewPropagationPolicy = ViewPropagationPolicy.PUSHPULL;
	private final static PeerSelectionPolicy peerSelectionPolicy = PeerSelectionPolicy.RAND;
	private final static int Tpss = 250;
	private final static int A = 1000;
	private final static int B = 6000;

	// DIAS Service Parameterization
	private final static int Tdias = 1000;
	private final static int Tsampling = 500; //pulling info from PSS to DIAS locally
	private final static int sampleSize = 10; //number of nodes
	private final static int numOfSessions = 10;
	private final static int unexploitedSize = 10;
	private final static int outdatedSize = 10;
	private final static int exploitedSize = 10;
	private final static AggregationStrategy.Strategy strategy = AggregationStrategy.Strategy.EXPLOITATION;
	private final static BloomFilterType amsType = BloomFilterType.COUNTING;
	private final static int amsHashType = CHashFactory.DOUBLE_HASH;
	private final static int ams_m = 16;
	private final static int ams_k = 24;
	private final static int dmaHashType = CHashFactory.DOUBLE_HASH;
	private final static int dma_m = 16;
	private final static int dma_k = 24;
	private final static int amdHashType = CHashFactory.DOUBLE_HASH;
	private final static int amd_m = 16;
	private final static int amd_k = 24;
	private final static int smaHashType = CHashFactory.DOUBLE_HASH;
	private final static int sma_m = 16;
	private final static int sma_k = 24;
	private final static Map<BloomFilterParams, Object> bfParams = new HashMap<BloomFilterParams, Object>();

	// DIAS Application Parameterization
	private final static AggregationType type = AggregationType.ARITHMETIC;
	private final static int Tboot = 15000;
	private final static int Taggr = 500000;
	private final static int k = 5;
	private final static double minValueDomain = 0;
	private final static double maxValueDomain = 1;
	private final static double Pt = 1.0;
	private final static double Ps = 1.0;
	private final static int t = 100000;
	private final static GenerationScheme genScheme = GenerationScheme.BETA;
	private final static SelectionScheme selScheme = SelectionScheme.CYCLICAL;

	public static void main(String[] args) throws UnknownHostException {
		expID = args[0];
    	//required because measurement dumper does not dump if folder doesn't exist
    	new File(expID).mkdirs();
		// env setup (configfile)
		Experiment.initEnvironment();
		// take the peer index from the second command-line argument
		MainConfiguration.getSingleton().peerIndex = Integer.parseInt(args[1]);
		// take the port number to bind to from the third command-line argument
		
		MainConfiguration.getSingleton().peerPort = Integer.parseInt(args[2]);
		
		if(args.length==4) {
			System.out.println("Setting peerIP to "+args[3]);
			MainConfiguration.getSingleton().peerIP = InetAddress.getByName(args[3]);
		}
		//MainConfiguration.getSingleton().peerIP = InetAddress.getLocalHost();
		int initial_degree = MainConfiguration.getSingleton().initialNodeDegree;
		System.out.println("Initial degree to parametrize DIAS "+initial_degree);
		final int dyn_c = initial_degree;
		final int dyn_S = Math.min(2, (initial_degree / 2) );
		final int dyn_samplesize = initial_degree;
		
		// peer setup (from configfile)
		final DIASLiveExperiment dias_experiment = new DIASLiveExperiment();
		dias_experiment.init();

		PeerFactory peerFactory = new PeerFactory() {
			public Peer createPeer(int peerIndex, Experiment experiment) {
				Peer newPeer = new Peer(peerIndex);
				if (peerIndex == 0) {
					newPeer.addPeerlet(new BootstrapServerUniform());
					// MonintorServer
				}
				newPeer.addPeerlet(new NeighborManager());
				newPeer.addPeerlet(new SimpleConnector());
				// MonitorClient
				newPeer.addPeerlet(new BootstrapClient(Experiment.getSingleton().getAddressToBindTo(0),
						new SimplePeerIdentifierGenerator()));
				
				newPeer.addPeerlet(new PeerSamplingService(dyn_c, H, dyn_S, peerSelectionPolicy, viewPropagationPolicy, Tpss,
						A, B));
				newPeer.addPeerlet(new DIAS(expID, Tdias, numOfSessions, Tsampling, dyn_samplesize, strategy,
						dyn_samplesize, dyn_samplesize, dyn_samplesize, collectBloomFilterParams()));
				newPeer.addPeerlet(new SimpleDIASApplication(expID, Tboot, Taggr, k, minValueDomain, maxValueDomain, t,
						Pt, Ps, genScheme, selScheme, type));
				return newPeer;
			}
		};

		int myPeerIndex = MainConfiguration.getSingleton().peerIndex;
		dias_experiment.initPeers(myPeerIndex, 1, peerFactory);
		dias_experiment.startPeers(myPeerIndex, 1);
		System.out.println("Started Peer "+myPeerIndex+ " on "+dias_experiment.getPeers().elementAt(myPeerIndex).getNetworkAddress());
	}

	private static Map<BloomFilterParams, Object> collectBloomFilterParams() {
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
