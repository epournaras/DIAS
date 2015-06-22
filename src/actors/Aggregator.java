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
package actors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import protopeer.Finger;
import aggregation.AggregationState;
import bloomfilter.CBloomFilter;
import bloomfilter.CCountingFilter;
import bloomfilter.CHashFactory;
import bloomfilter.IFilter;
import consistency.AggregationOutcome;
import consistency.AggregatorReport;
import consistency.BloomFilterParams;
import consistency.DisseminatorReport;
import dsutil.generic.state.ArithmeticState;
import dsutil.generic.state.State;
import dsutil.generic.state.StateException;
import dsutil.protopeer.services.aggregation.AggregationType;

/**
 * The aggregator stores the aggregates and the aggregation memberships for the
 * consistency checks. Memberships are stored in two types of bloom filters:
 * 
 * State Memberships in the Aggregate (SMA)
 * Disseminator Memberships in the Aggregator (DMA)
 *
 * SMA is a counting bloom filter and is the one that defines which states have
 * been counted in the aggregates and which not. DMA is a simple bloom filter
 * that tracks the disseminator memberships that have contributed to the aggregate
 * calculation. The bloom filters are initialized with system parameters.
 * Information about the items in the bloom filters is also preserved for
 * measuring the probabilities of false positives.
 *
 * The aggregator receives reports from disseminators with their local values
 * and membership information about the aggregation. Based on this information,
 * it updates its aggregation functions.
 *
 * The consistency check that the aggregator performs is based on mutual
 * membership information with the disseminators. If mutual memberships are not
 * confirmed, the aggregation is not performed. This elliminates the effect of
 * false positives but also of false negatives that can appear after removals
 * of false positives in counting bloom filters.
 *
 *
 * @author Evangelos
 */
public class Aggregator {

    private AggregationState aggregates;
    private CCountingFilter SMA;
    private CBloomFilter DMA;
    private int SMACounter;
    private int DMACounter;
    private HashMap<BloomFilterParams, Object> parameters;
    
    /**
     * Initializes the aggregator with the bloom filter parameters.
     *
     * @param parameters the parameters of the SMA and DMA bloom filters
     */
    public Aggregator(AggregationType type, Map<BloomFilterParams, Object> parameters){
        this.init(type, parameters);
    }

    /**
     * Aggregator is initialized by:
     *
     * 1. storing the bloom filter parameters
     * 2. Creating an aggregation state
     * 3. Creating a SMA bloom filter and its counter
     * 4. Creating a DMA bloom filter and its counter
     *
     * @param parameters the parameters of the SMA and DMA bloom filters
     */
    public void init(AggregationType type, Map<BloomFilterParams, Object> parameters){
        this.parameters=new HashMap<BloomFilterParams, Object>();
        this.parameters.putAll(parameters);
        aggregates=new AggregationState(type);
        this.SMA=new CCountingFilter(this.initSMA());
        this.SMACounter=0;
        this.DMA=new CBloomFilter(this.initDMA());
        this.DMACounter=0;
    }

    /**
     * Creates and parameterizes a hash factory for the SMA bloom filter.
     *
     * @return a hash factory the parameterizes the SMA bloom filter
     */
    private CHashFactory initSMA(){
        int type=((Integer)parameters.get(BloomFilterParams.SMA_HASH_TYPE)).intValue();
        int m=((Integer)parameters.get(BloomFilterParams.SMA_M)).intValue();
        int k=((Integer)parameters.get(BloomFilterParams.SMA_K)).intValue();
        return new CHashFactory(type, m, k);
    }

    /**
     * Creates and parameterizes a hash factory for the DMA bloom filter.
     *
     * @return a hash factory the parameterizes the DMA bloom filter
     */
    private CHashFactory initDMA(){
        int type=((Integer)parameters.get(BloomFilterParams.DMA_HASH_TYPE)).intValue();
        int m=((Integer)parameters.get(BloomFilterParams.DMA_M)).intValue();
        int k=((Integer)parameters.get(BloomFilterParams.DMA_K)).intValue();
        return new CHashFactory(type, m, k);
    }

    /**
     * Adds a disseminator membership in the DMA bloom filter if it is not present and
     * increments the DMA counter.
     *
     * @param disseminator the disseminator membership to be added in the DMA
     *
     * @return true if the disseminator membership is added in the DMA bloom filter
     */
    public boolean addDMAMembership(Finger disseminator){
        if(!this.DMA.contains(disseminator.toString())){
            this.DMA.add(disseminator.toString());
            this.DMACounter++;
            return true;
        }
        return false;
    }

    /**
     * Adds a selected state membership in the SMA bloom filter if it is not present and
     * increments the SMA counter.
     *
     * @param state the selected state membership to be added in the SMA
     *
     * @return true if the selected state membership is added in the SMA bloom filter
     */
    public boolean addSMAMembership(State state){
        if(!this.SMA.contains(state.getStateId().toString())){
            this.SMA.add(state.getStateId().toString());
            this.SMACounter++;
            return true;
        }
        return false;
    }

    /**
     * Adds a slected state in the aggregation state
     *
     * @param state the added state
     */
    public void addAggregationState(State state){
        try{
            this.aggregates.addState(state);
        }
        catch(StateException ex){
            System.out.println(ex.toString()+ex.getStateExcMsg());
        }
    }

    /**
     * Removes a selected state from the aggregation state
     *
     * @param state the removed state
     */
    public void removeAggregationState(State state){
        try{
            this.aggregates.removeState(state);
        }
        catch(StateException ex){
            System.out.println(ex.toString()+ex.getStateExcMsg());
        }
    }

    /**
     * Removes a selected state membership from the SMA bloom filter if it is
     * present and decrements the SMA counter
     *
     * @param state the removed selected state membership
     *
     * @return true if the selected state membership is removed from the SMA bloom
     * filter
     */
    public boolean removeSMAMembership(State state){
        if(this.SMA.contains(state.getStateId().toString())){
            this.SMA.remove(state.getStateId().toString());
            this.SMACounter--;
            return true;
        }
        return false;
    }

    /**
     * Accesses the aggregation state with the calculated aggregation functions
     *
     * @return the aggregation state of the aggregator
     */
    public AggregationState getAggregationState(){
        return this.aggregates;
    }

    /**
     * @return the SMACounter
     */
    public int getSMACounter() {
        return SMACounter;
    }

    /**
     * @return the DMACounter
     */
    public int getDMACounter() {
        return DMACounter;
    }

    /**
     * Removes all the elements from:
     *
     * 1. the aggregation state
     * 2. the SMA bloom filter
     * 3. the DMA bloom filter
     *
     * and it resets the SMA and DMA counters.
     */
    public void clearAggregates(){
        this.aggregates.resetAggregationState(this.aggregates.getAggregationType());
        this.SMA.clear();
        this.SMACounter=0;
        this.DMA.clear();
        this.DMACounter=0;
    }

    /**
     * Calculates the false positive probability given a bloom filter with a
     * certian number of items.
     *
     * @param filter a bloom filter
     * @param n the number of elemets added in the bloom filter
     */
    public double getFalsePositiveProbability(IFilter filter, int n){
        double k=filter.hash().k();
        double m=filter.container().size();
        double p=Math.pow((1-Math.pow(Math.E, -k*n/m)), k);
        return p;
    }

    /**
     * Calculates the false positive probabilities for the DMA bloom filter
     *
     * @return the false positive probability of the DMA bloom filter
     */
    public double getDMAFalsePositiveProbability(){
        return this.getFalsePositiveProbability(this.DMA, getDMACounter());
    }

    /**
     * Calculates the false positive probabilities for the SMA bloom filter
     *
     * @return the false positive probability of the SMA bloom filter
     */
    public double getSMAFalsePositiveProbability(){
        return this.getFalsePositiveProbability(this.SMA, getSMACounter());
    }

    /**
     * Returns the state that corresponds to the AMS bloom filter with the lower
     * probability of false positives
     * 
     * @param posAMS the states of the AMS bloom filters with positive membership
     * in the aggregator
     * @param posASMFalsePos a map with the corresponding states of the AMS
     * bloom filters and their false positives probabilities
     *
     * @return the possible state with the minimum false positive in the corresponding
     * AMS bloom filter
     */
    private State getMinFPState(HashSet<State> posAMS, HashMap<UUID, Double> posAMSFalsePos){
        State minFPState=null;
        for(State state:posAMS){
            if(minFPState==null){
                minFPState=state;
            }
            double s=((Double)posAMSFalsePos.get(state.getStateId())).doubleValue();
            double min=((Double)posAMSFalsePos.get(minFPState.getStateId())).doubleValue();
            if(s<min){
                minFPState=state;
            }
        }
        return minFPState;
    }

    /**
     * Adds states in the aggregation state by satisfying two conditions:
     *
     * 1. No double counts
     * 2. Update of old selected states with the new ones.
     *
     * This is satisfied my mutual membership checks in the bloom filters of the
     * aggregator and the disseminator. More specifically the check algorithm
     * works as follows:
     *
     * 1. Check if there is a mutual memberhsip in the AMD and DMA bloom filters.
     * This satisfies that the two peers have aggregated before or not.
     *
     *      1.1 If there is a membership, the aggregator removes the positive
     *      memberhsip of aggregators in the AMS that do not have a membership
     *      in the DMA. This actions corresponds to the removal of false positives
     *      generated from the AMS.
     *
     *      1.2 If there is one and only one membership of aggregators in AMS left,
     *
     *          1.2.1 If the respective state corresponding to this membership
     *          is different from the new state, it is replaced, otherwise the
     *          aggregates are updated for their double insensitive aggregation
     *          functions: MAX and MIN
     *
     *      otherwise the aggregator cannot decide which memberships are false
     *      positive or not and therefore aggregation cannot be performed. An
     *      alternative scenario could be to check the false positive
     *      probabilities and choose the one with the minimum value.
     *
     * if there is no mutual membership, this means that, for sure, the two nodes
     * exchange information for first time. Therefore the selected state is
     * added in the aggregate.
     *
     * @param disseminator the disseminator from which the report is received
     * @param disseminatorReport the report of the disseminator informing about its
     * current local state
     *
     * @return the aggregator report
     */
    public HashMap<AggregatorReport, Object> receiveDisseminatorReport(Finger disseminator, HashMap<DisseminatorReport, Object> disseminatorReport){
        HashMap<AggregatorReport, Object> aggregatorReport=new HashMap<AggregatorReport, Object>();
        boolean ack=false;
        State AMSRemoval=null;
        State AMSAddition=null;
        AggregationOutcome outcome=null;
        State newState=(State)disseminatorReport.get(DisseminatorReport.SELECTED_STATE);
        
        HashMap<UUID, Double> posAMSFalsoPos=(HashMap<UUID, Double>)disseminatorReport.get(DisseminatorReport.POSITIVE_AMS_FP);
        double AMDFalsePos=((Double)disseminatorReport.get(DisseminatorReport.AMD_FP)).doubleValue();
        boolean positiveAMD=((Boolean)disseminatorReport.get(DisseminatorReport.POSITIVE_AMD)).booleanValue();
        boolean positiveDMA=this.DMA.contains(disseminator.toString());
        
        // Level 1: Aggregation has been performed before
        if(positiveAMD && positiveDMA){
            // Level 1.1: Check for false positives in the positive AMS memberships
            HashSet<State> posAMS=(HashSet)disseminatorReport.get(DisseminatorReport.POSITIVE_AMS);
            Iterator<State> it=posAMS.iterator();
            while(it.hasNext()){
                State state=it.next();
                if(!this.SMA.contains(state.getStateId().toString())){
                    it.remove();
                }
            }
//            if(posAMS.size()==0){
//                AMSAddition=newState;
//                this.addDMAMembership(disseminator);
//                this.addSMAMembership(newState);
//                this.addAggregationState(newState);
//                outcome=AggregationOutcome.FIRST;
//                ack=true;
//            }
//            else{
                // Level 1.2: Perform an aggregation if and only if there only one
                // mutual membership between AMS and SMA
                if(posAMS.size()==1){
                    // Level 1.2.1: Perform a replacement if the new state is
                    // different compared to the old one.
                    State oldState=posAMS.iterator().next();
                    if(!oldState.equals(newState)){
                        AMSRemoval=oldState;
                        this.removeSMAMembership(oldState);
                        this.removeAggregationState(oldState);
                        AMSAddition=newState;
                        this.addSMAMembership(newState);
                        this.addAggregationState(newState);
                        outcome=AggregationOutcome.REPLACE;
                        ack=true;
                    }
                    else{
                        try{
                            this.aggregates.updateMaxMin(newState);
                        }
                        catch(StateException ex){
                            System.out.println(ex.toString()+ex.getStateExcMsg());
                        }
                        outcome=AggregationOutcome.DOUBLE;
                        ack=true;
                    }

                }
                else{
    //                // Another approach-start
    //                State oldState=this.getMinFPState(posAMS, posAMSFalsoPos);
    //                if(!oldState.equals(newState)){
    //                    AMSRemoval=oldState;
    //                    this.removeSMAMembership(oldState);
    //                    this.removeAggregationState(oldState);
    //                    AMSAddition=newState;
    //                    this.addSMAMembership(newState);
    //                    this.addAggregationState(newState);
    //                    outcome=AggregationOutcome.UNSUCCESSFUL;
    //                    ack=true;
    //                }
    //                else{
    //                    try{
    //                        this.aggregates.updateMaxMin(newState);
    //                    }
    //                    catch(StateException ex){
    //                        System.out.println(ex.toString()+ex.getStateExcMsg());
    //                    }
    //                    outcome=AggregationOutcome.UNSUCCESSFUL;
    //                    ack=true;
    //                }
    //                // Another approach-end
                    outcome=AggregationOutcome.UNSUCCESSFUL;
                    ack=false;
                }
//            }
        }
        else{
            AMSAddition=newState;
            this.addDMAMembership(disseminator);
            this.addSMAMembership(newState);
            this.addAggregationState(newState);
            outcome=AggregationOutcome.FIRST;
            ack=true;
        }
        aggregatorReport.put(AggregatorReport.ACK, ack);
        aggregatorReport.put(AggregatorReport.OUTCOME, outcome);
        aggregatorReport.put(AggregatorReport.AMS_ADDITION, AMSAddition);
        aggregatorReport.put(AggregatorReport.AMS_REMOVAL, AMSRemoval);
        return aggregatorReport;
    }
}
