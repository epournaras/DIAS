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

import dsutil.protopeer.services.aggregation.AggregationFunction;
import dsutil.protopeer.services.aggregation.AggregationType;
import dsutil.generic.state.ArithmeticState;
import cern.jet.random.Beta;
import cern.jet.random.engine.MersenneTwister;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.media.jai.util.Range;

import org.apache.log4j.Logger;

import peerlets.DIASInterface;
import protopeer.BasePeerlet;
import protopeer.Peer;
import dsutil.generic.state.State;
import dsutil.protopeer.services.aggregation.AggregationInterface;
import protopeer.measurement.MeasurementFileDumper;
import protopeer.measurement.MeasurementLog;
import protopeer.measurement.MeasurementLoggerListener;
import protopeer.network.Message;
import protopeer.time.Timer;
import protopeer.time.TimerListener;
import protopeer.util.quantities.Time;


/**
 * This is an use-case example of a DIAS application. It simulates the generation
 * of random possible states and the selection of one of them. The selection
 * changes according to a probability periodically. The application also requests
 * an aggregation by DIAS periodically. The aggregates are received and tracked
 * for the further evaluation.
 *
 * @author Evangelos
 */
public class SimpleDIASApplication extends BasePeerlet implements DIASApplicationInterface{

    private AggregationType aggregationType;
    private State selectedState;
    private ArrayList<State> possibleStates;
    //private MeasurementFileDumper dumper;
    private int Tboot;
    private int Taggr;
    private String id;
    private int k;
    private Range domain;
    private double Ps;
    private double Pt;
    private int t;
    private Time randomInterval;
    private GenerationScheme genScheme;
    private SelectionScheme selScheme;
    private int ind;

    private double avg;
    private double sum;
    private double sumsqr;
    private double max;
    private double min;
    private double stdev;
    private double count;
    
    private static final Logger logger = Logger.getLogger(SimpleDIASApplication.class);

    /**
     * Initializes a DIAS application.
     *
     * @param id the local experiment identifier
     * @param Tboot a bootstrapping period before requesting an aggregation
     * @param Taggr the period of aggregation request
     * @param k the number of possible states
     * @param minValue the minimum value that can be assigned to a possible state
     * @param maxValue the maximum value that can be assigned to a possible state
     * @param t the period of evaluation for changing a selected state
     * @param Pt the probability of changing a selected state every time t
     * @param Ps the probability of changing the state of a parameter s
     * @param genScheme the generation scheme of possible states
     * @param selScheme the selection scheme of the selected state from the possible
     * states
    */
    public SimpleDIASApplication(String id, int Tboot, int Taggr, int k, double minValue, double maxValue, int t, double Pt, double Ps, GenerationScheme genScheme, SelectionScheme selScheme, AggregationType type){
        this.possibleStates=new ArrayList<State>();
        this.initAggregates();
        this.Tboot=Tboot;
        this.Taggr=Taggr;
        this.id=id;
        this.k=k;
        this.domain=new Range(Double.class, minValue, true, maxValue, false);
        this.t=t;
        this.Pt=Pt;
        this.Ps=Ps;
        this.randomInterval=Time.inMilliseconds(1000);
        this.genScheme=genScheme;
        this.selScheme=selScheme;
        this.aggregationType=type;
    }

    /**
    * Intitializes the DIAS application
    *
    * @param peer the local peer
    */
    @Override
    public void init(Peer peer){
        super.init(peer);
        this.id=this.id+'/'+getPeer().getIdentifier().toString();
        this.ind = getPeer().getIndexNumber();
    }

    /**
     * Starts the DIAS application by generating the possible states and selecting
     * one of them. The runtime is scheduled. Specifically, the measurements,
     * the aggregation requests and the state dynamics are scheduled.
    */
    @Override
    public void start(){
        this.possibleStates.addAll(this.generatePossibleStates());
        this.selectedState=this.selectPossibleState();
        this.scheduleMeasurements();
        this.runBootstrap();
        this.runStateDynamics();
        logger.debug("started peer " + this.id + " with state: " + getSelectedState());

    }

    /**
     * Stops the DIAS application
    */
    @Override
    public void stop(){

    }

    /**
     * Accesses the DIAS aggregation service
    */
    private AggregationInterface getAggregationInterface(){
        return (AggregationInterface)getPeer().getPeerletOfType(DIASInterface.class);
    }

    /**
     * Bootstraps the aggregation requests at Tboot.
    */
    private void runBootstrap(){
        Timer bootstrapTimer= getPeer().getClock().createNewTimer();
        bootstrapTimer.addTimerListener(new TimerListener(){
            public void timerExpired(Timer timer){
                getAggregationInterface().requestAggregation(aggregationType, possibleStates, selectedState);
                runAggregation();
            }
        });
        bootstrapTimer.schedule(Time.inMilliseconds(this.Tboot-this.getRandomInterval(randomInterval)));
   }

    /**
     * Periodically, the application instance requests an aggregation by the DIAS
     * service. The application collects the aggregates before requests a new
     * aggregation, generates possible states, selects one of them and finally
     * requests a new aggregation.
    */
    private void runAggregation(){
        Timer aggregationTimer= getPeer().getClock().createNewTimer();
        aggregationTimer.addTimerListener(new TimerListener(){
            public void timerExpired(Timer timer){
                getAggregates(getAggregationInterface());
                possibleStates.clear();
                selectedState=null;
                possibleStates.addAll(generatePossibleStates());
                selectedState=selectPossibleState();
                getAggregationInterface().requestAggregation(aggregationType, possibleStates, selectedState);
                runAggregation();
            }
        });
        aggregationTimer.schedule(Time.inMilliseconds(this.Taggr-this.getRandomInterval(randomInterval)));
   }

    /**
     * Periodically the state selection changes according to the dynamics probability.
     * The DIAS service is updated with the new selection.
    */
    private void runStateDynamics(){
        Timer dynamicTimer= getPeer().getClock().createNewTimer();
        dynamicTimer.addTimerListener(new TimerListener(){
            public void timerExpired(Timer timer){
                if(changeSelectedState()){
                    getAggregationInterface().changeSelectedState(selectedState);
                }
                runStateDynamics();
            }
        });
        dynamicTimer.schedule(Time.inMilliseconds(this.t-this.getRandomInterval(randomInterval)));
    }

    /**
     * Initializes all the aggregates with zero values.
    */
    private void initAggregates(){
        this.avg=0.0;
        this.sum=0.0;
        this.max=0.0;
        this.min=0.0;
        this.sumsqr=0.0;
        this.count=0.0;
        this.stdev=0.0;
    }

    /**
     * Generates a +/-r random interval for simulatiing asychronous operations.
     *
     * @param seed the average seed time interval that is added or removed
     *
     * @return the random interval in milliseconds
    */
    private double getRandomInterval(Time seed){
        return (Math.random()-0.5)*Time.inMilliseconds(seed);
    }

    /**
     * Accesses the DIAS service and receives the estimated aggregates.
     *
     * @param aggregator the DIAS aggregator
    */
    private void getAggregates(AggregationInterface aggregator){
        if(aggregator.isActive()){
            this.avg=(Double)aggregator.getAggregate(AggregationFunction.AVG);
            this.sum=(Double)aggregator.getAggregate(AggregationFunction.SUM);
            this.sumsqr=(Double)aggregator.getAggregate(AggregationFunction.SUM_SQR);
            this.stdev=(Double)aggregator.getAggregate(AggregationFunction.STDEV);
            this.max=(Double)aggregator.getAggregate(AggregationFunction.MAX);
            this.min=(Double)aggregator.getAggregate(AggregationFunction.MIN);
            this.count=(Double)aggregator.getAggregate(AggregationFunction.COUNT);
            //System.out.println(this.id+" "+System.currentTimeMillis()+" "+avg+" "+stdev+" "+min+" "+max+" "+count);
            //System.out.println(this.id+" "+System.currentTimeMillis()+" "+this.max + " " + this.avg);
            logger.debug("idx"+ this.ind + " sum: " +this.sum +" cnt: "+this.count);
        }
    }

    /**
     * Accesses the possible states of the application
     *
     * @return the possible states
    */
    public Collection<State> getPossibleStates(){
       return possibleStates;
    }

    /**
     * Accesses the selected states of the application
     *
     * @return the selected states
    */
    public State getSelectedState(){
        return this.selectedState;
    }

    /**
     * Generates a number of possible states
     *
     * @return a collection of possible states
    */
    public Collection<State> generatePossibleStates(){
        switch(this.genScheme){
            case RANDOM:
                return this.generateRandomStates(k, domain);
            case UNIFORM:
                return this.generateUniformStates(k, domain);
            case BETA:
                return this.generateBetaDistributionStates(k, domain);
            default:
                // other selection and generation genScheme
                return null;
        }
        
    }

    /**
     * Selects a state from the possible states
     *
     * @return a collection of states
    */
    public State selectPossibleState(){
        switch(this.selScheme){
            case RANDOM:
                return this.selectRandomState();
            case CYCLICAL:
                return this.selectCyclicalState();
            default:
                // other selection and generation genScheme
                return null;
        }
    }

    /**
     * Selects randomly a state from the possible states.
     *
     * @return a random state
    */
    private State selectRandomState(){
        ArrayList<State> states=new ArrayList<State>();
        states.addAll(possibleStates);
        states.remove(selectedState);
        int l=states.size();
        return states.get((int)(Math.random()*l));
    }

    /**
     * Selects cyclically a state from the possible states.
     *
     * @return a cyclical state
    */
    private State selectCyclicalState(){
        int index=this.possibleStates.indexOf(this.selectedState);
        if(index==this.possibleStates.size()-1){
            return this.possibleStates.get(0);
        }
        else{
            return this.possibleStates.get(index+1);
        }
        
    }

    /**
     * Generates random arithmetic states given an input domain of values. The
     * random values are first generated in the range [0,1) and then are transfered
     * proportionally in the input domain range according to the following formula:
     *
     * (r-0)/(1-r)=(x-min)/(max-x) =>
     * r*max-r*x=x-min-r*x+r*min =>
     * x=r*max+min-r*min
     *
     * @param k the number of possible states for generation
     * @param domain the input domain the the values of the states
     *
     * @return a set with random states
    */
    private Set<State> generateRandomStates(int k, Range domain){
        Set<State> randomStates=new HashSet<State>();
        for(int i=0; i<k; i++){
            double r=Math.random();
            double min=((Double)domain.getMinValue()).doubleValue();
            double max=((Double)domain.getMaxValue()).doubleValue();
            double x=r*max+min-r*min;
            ArithmeticState state=new ArithmeticState(x);
            randomStates.add(state);
        }
        return randomStates;
    }

    /**
     * Generates uniform arithmetic states given an input domain of values. The
     * uniform values are first generated in the range [0,1) and then are transfered
     * proportionally in the input domain range according to the following formula:
     *
     * (r-0)/(1-r)=(x-min)/(max-x) =>
     * r*max-r*x=x-min-r*x+r*min =>
     * x=r*max+min-r*min
     *
     * @param k the number of possible states for generation
     * @param domain the input domain for the values of the states
     *
     * @return a set with uniform states
    */
    private List<State> generateUniformStates(int k, Range domain){
        ArrayList<State> uniformStates=new ArrayList<State>();
        double min=((Double)domain.getMinValue()).doubleValue();
        double max=((Double)domain.getMaxValue()).doubleValue();
        double increment=(max-min)/(double)k;
        double value=increment;
        for(int i=0; i<k; i++){
            double x=value*max+min-value*min;
            ArithmeticState state=new ArithmeticState(x);
            uniformStates.add(state);
            value=value+increment;
        }
        return uniformStates;
    }

    /**
     * Generates random values according to the beta distribution. There are various
     * distributions generated according to the alpha and beta parameters. The
     * beta values are first generated in the range [0,1) and then are transfered
     * proportioally in the input domain range according to the following formula:
     *
     * (r-0)/(1-r)=(x-min)/(max-x) =>
     * r*max-r*x=x-min-r*x+r*min =>
     * x=r*max+min-r*min
     *
     * @param k the number of possible states for generation
     * @param domain the input domain for the values of the states
     *
     * @return a set with beta distribution states
    */
    private List<State> generateBetaDistributionStates(int k, Range domain){
        ArrayList<State> betaDistrStates=new ArrayList<State>();
        double min=((Double)domain.getMinValue()).doubleValue();
        double max=((Double)domain.getMaxValue()).doubleValue();
        double value=0.0;
        double x=0.0;

        Beta beta01=new Beta(25.0, 5.0, new MersenneTwister((int)(Math.random()*1073741823)));
        value=beta01.nextDouble();
        x=value*max+min-value*min;
        ArithmeticState beta01State=new ArithmeticState(x);
        betaDistrStates.add(beta01State);

        Beta beta02=new Beta(5.0, 25.0, new MersenneTwister((int)(Math.random()*1073741823)));
        value=beta02.nextDouble();
        x=value*max+min-value*min;
        ArithmeticState beta02State=new ArithmeticState(x);
        betaDistrStates.add(beta02State);

        Beta beta03=new Beta(10.0, 5.0, new MersenneTwister((int)(Math.random()*1073741823)));
        value=beta03.nextDouble();
        x=value*max+min-value*min;
        ArithmeticState beta03State=new ArithmeticState(x);
        betaDistrStates.add(beta03State);

        Beta beta04=new Beta(5.0, 10.0, new MersenneTwister((int)(Math.random()*1073741823)));
        value=beta04.nextDouble();
        x=value*max+min-value*min;
        ArithmeticState beta04State=new ArithmeticState(x);
        betaDistrStates.add(beta04State);

        Beta beta05=new Beta(5.0, 5.0, new MersenneTwister((int)(Math.random()*1073741823)));
        value=beta05.nextDouble();
        x=value*max+min-value*min;
        ArithmeticState beta05State=new ArithmeticState(x);
        betaDistrStates.add(beta05State);

        return betaDistrStates;
    }

    /**
     * It performs a new selection of possible states according to the dynamics
     * probability: Ps*Pt
     *
     * @return true if a new selection is performed
    */
    double r=Math.random();
    public boolean changeSelectedState(){
        if(r<Ps*Pt){
            this.selectedState=this.selectPossibleState();
            return true;
        }
        return false;
    }

     /**
     * Scheduling the measurements for DIAS. The aggregates from DIAS service
     * are logged in this case.
     */
    private void scheduleMeasurements(){
    	//System.out.println("SimpleDiasApplication dumper disabled!");
        //dumper=new MeasurementFileDumper(id);
        getPeer().getMeasurementLogger().addMeasurementLoggerListener(new MeasurementLoggerListener(){
            public void measurementEpochEnded(MeasurementLog log, int epochNumber){
                getAggregates(getAggregationInterface());
                log.log(epochNumber, AggregationFunction.AVG, avg);
                log.log(epochNumber, AggregationFunction.SUM, sum);
                log.log(epochNumber, AggregationFunction.SUM_SQR, sumsqr);
                log.log(epochNumber, AggregationFunction.MAX, max);
                log.log(epochNumber, AggregationFunction.MIN, min);
                log.log(epochNumber, AggregationFunction.STDEV, stdev);
                log.log(epochNumber, AggregationFunction.COUNT, count);
                log.log(epochNumber, "SELECTION", ((ArithmeticState)selectedState).getValue());
                //dumper.measurementEpochEnded(log, epochNumber);
                log.shrink(epochNumber, epochNumber+1);
            }
        });
    }   
}
