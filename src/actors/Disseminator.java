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

import bloomfilter.CBloomFilter;
import bloomfilter.CCountingFilter;
import bloomfilter.CHashFactory;
import bloomfilter.IFilter;
import consistency.AggregatorReport;
import consistency.BloomFilterParams;
import consistency.BloomFilterType;
import consistency.DisseminatorReport;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import protopeer.Finger;
import dsutil.generic.state.State;

/**
 * The disseminator is the initiator actor of the aggregation. It provides
 * information about the selected state of an application. It also stores
 * membership information for maintaining consistency in the distributed
 * aggregation.
 * 
 * Membership information is stored in bloom filters. There are two types of
 * bloom filters:
 * 
 * Aggregator Memberships in the Disseminator (AMD)
 * Aggregator Memberships of a State (AMSs)
 * 
 * AMD is a simple bloom filter that tracks which aggregators have selected a
 * state of the disseminator to estimate their aggregates. 
 * 
 * AMSs can a simple or counting bloom filter for every possible state of a
 * a disseminator. AMSs store membership of aggregators. In case of a simple
 * bloom filter, remocals are not permitted. This means that there is information
 * about all the previous selections that an aggregatator performed and not only the
 * current one. In case of a counting bloom filter, removals are allowed. The
 * memberships express which aggregators are informed about the current selection
 * of the disseminator. 
 * 
 * The disseminator also counts the number of items stored in each bloom filter.
 * This can be helpful to calculate the probability of false positives in the
 * filters. 
 * 
 * States and state selections can change on demand. Memberships are adjusted 
 * based on the received report of the aggregator.
 *
 * @author Evangelos
 */
public class Disseminator {

    private State selectedState;
    private HashSet<State> possibleStates;
    private CBloomFilter AMD;
    private HashMap AMSs;
    private HashMap<UUID, Integer> AMSCounters;
    private int AMDCounter;
    private HashMap<BloomFilterParams, Object> parameters;

    /**
     * Initializes the disseminator with a number of parameters.
     *
     * @param possibleStates the possible states that an application can select
     * @param selectedState the selected state from the possible ones
     * @param parameters the parameterization of the bloom filters
     */
    public Disseminator(Collection<State> possibleStates, State selectedState, Map<BloomFilterParams, Object> parameters){
        this.init(possibleStates, selectedState, parameters);
    }

    /**
     * Initializes the disseminator as follows:
     *
     * 1. Sets the selected state.
     * 2. Sets the possible state.
     * 3. Stores the bloom filter parameters
     * 4. Creates a AMSs for every possible state
     * 5. Creates an AMD
     * 6. Creates the AMSs counters
     * 7. Creates the AMD counter
     *
     * @param possibleStates the possible states that an application can select
     * @param selectedState the selected state from the possible ones
     * @param parameters the parameterization of the bloom filters
     */
    private void init(Collection<State> possibleStates, State selectedState, Map<BloomFilterParams, Object> parameters){
        // 1. Sets the selected state.
        this.selectedState=selectedState;
        // 2. Sets the possible state.
        this.possibleStates=new HashSet<State>();
        this.possibleStates.addAll(possibleStates);
        // 3. Stores the bloom filter parameters
        this.parameters=new HashMap<BloomFilterParams, Object>();
        this.parameters.putAll(parameters);
        // 4. Creates a AMSs for every possible state
        switch(this.getBloomFilterType()){
            case SIMPLE:
                this.AMSs=new HashMap<State, CBloomFilter>();
                break;
            case COUNTING:
                this.AMSs=new HashMap<State, CCountingFilter>();
                break;
            default:
                // other types of bloom filters
        }
        this.createAMSs();
        // 5. Creates an AMD
        this.AMD=new CBloomFilter(this.initAMD());
        // 6. Creates the AMSs counters
        this.AMSCounters=new HashMap<UUID, Integer>();
        Iterator<State> it=this.possibleStates.iterator();
        while(it.hasNext()){
            this.getAMSCounters().put(it.next().getStateId(), 0);
        }
        // 7. Creates the AMD counter
        this.AMDCounter=0;
    }

    /**
     * Clears the AMD and AMSs and resets the counters.
     */
    public void clearMemberships(){
        this.AMD.clear();
        this.AMDCounter=0;
        Iterator<State> it=this.AMSs.keySet().iterator();
        while(it.hasNext()){
            State state=it.next();
            switch(this.getBloomFilterType()){
                case SIMPLE:
                    CBloomFilter bf=(CBloomFilter)this.AMSs.get(state);
                    bf.clear();
                    break;
                case COUNTING:
                    CCountingFilter cf=(CCountingFilter)this.AMSs.get(state);
                    cf.clear();
                    break;
                default:
                    // other type of bloom filter
            }
            this.getAMSCounters().put(state.getStateId(), 0);
        }
    }

    /**
     * Creates and parameterizes a hash factory for the AMSs bloom filter.
     *
     * @return a hash factory the parameterizes the AMSs bloom filter
     */
    private CHashFactory initAMS(){
        int type=((Integer)parameters.get(BloomFilterParams.AMS_HASH_TYPE)).intValue();
        int m=((Integer)parameters.get(BloomFilterParams.AMS_M)).intValue();
        int k=((Integer)parameters.get(BloomFilterParams.AMS_K)).intValue();
        return new CHashFactory(type, m, k);
    }

    /**
     * Creates and parameterizes a hash factory for the AMDs bloom filter.
     *
     * @return a hash factory the parameterizes the AMDs bloom filter
     */
    private CHashFactory initAMD(){
        int type=((Integer)parameters.get(BloomFilterParams.AMD_HASH_TYPE)).intValue();
        int m=((Integer)parameters.get(BloomFilterParams.AMD_M)).intValue();
        int k=((Integer)parameters.get(BloomFilterParams.AMD_K)).intValue();
        return new CHashFactory(type, m, k);
    }

    /**
     * Creates an AMSs bloom filter (simple or counting) for every possible state
     */
    private void createAMSs(){
        Iterator<State> it=this.possibleStates.iterator();
        IFilter filter=null;
        while(it.hasNext()){
            switch(this.getBloomFilterType()){
                case SIMPLE:
                    filter=new CBloomFilter(this.initAMS());
                    break;
                case COUNTING:
                    filter=new CCountingFilter(this.initAMS());
                    break;
                default:
                    // other type of bloom filter
            }
            this.AMSs.put(it.next(), filter);
        }
    }

    /**
     * Checks the bloom filter parameters for the type used for AMS bloom filter
     *
     * @return the bloom filter type, SIMPLE or COUNTING
     */
    private BloomFilterType getBloomFilterType(){
        return (BloomFilterType)parameters.get(BloomFilterParams.AMS_TYPE);
    }

    /**
     * Adds an aggregator membership in the AMS bloom filter for a given state.
     *
     * @param state the state that an aggregator counts in its aggregate
     * @param aggregator the peer that counted the state of the local peer
     *
     * @return false iff the the state does not exist or there is already a
     * membership or the type of the bloom filter is inknown
     */
    public boolean addAMSMemebership(State state, Finger aggregator){
        if(this.AMSs.get(state)!=null){
            switch(this.getBloomFilterType()){
                case SIMPLE:
                    CBloomFilter bf=(CBloomFilter)this.AMSs.get(state);
                    if(bf.contains(aggregator.toString())){
                       return false;
                    }
                    bf.add(aggregator.toString());
                    break;
                case COUNTING:
                    CCountingFilter cf=(CCountingFilter)this.AMSs.get(state);
                    if(cf.contains(aggregator.toString())){
                        return false;
                    }
                    cf.add(aggregator.toString());
                    break;
                default:
                    return false;
                    // other type of bloom filter
            }
            Integer counter=((Integer)this.getAMSCounters().get(state.getStateId())).intValue()+1;
            this.getAMSCounters().put(state.getStateId(), counter);
            return true;
        }
        return false;
    }

    /**
     * Adds an aggregator membership in the AMD bloom filter.
     *
     * @param aggregator the aggregator membership added in AMD
     *
     * @return false if the membership of the aggregator does not exist
     */
    public boolean addAMDMembership(Finger aggregator){
        if(!this.AMD.contains(aggregator.toString())){
            this.AMD.add(aggregator.toString());
            this.AMDCounter++;
            return true;
        }
        return false;
    }

    /**
     * Removes a state membership from an AMS counting bloom filter given an
     * associated state.
     *
     * @param state the state from which an aggregator membership is removed
     * @param aggregator the aggregator membership to remove
     *
     * @return false if the AMS bloom filter is not counting or the membership
     * does not exist
     */
    public boolean removeAMSMembership(State state, Finger aggregator){
        if(this.getBloomFilterType()==BloomFilterType.COUNTING){
            CCountingFilter cf=(CCountingFilter)this.AMSs.get(state);
            if(cf.contains(aggregator.toString())){
                cf.remove(aggregator.toString());
                Integer counter=((Integer)this.getAMSCounters().get(state.getStateId())).intValue()-1;
                this.getAMSCounters().put(state.getStateId(), counter);
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * Changes the selected state as imposed by the application
     *
     * @param state the new selected state
     */
    public void setSelectedState(State state){
        this.selectedState=state;
    }

    /**
     * Returns the selected state
     *
     * @return the selected state of the aggregation application
     */
    public State getSelectedState(){
        return this.selectedState;
    }

    /**
     * Returns the number of elements n added in the AMD bloom filter
     *
     * @return n
     */
    public int getAMDCounter() {
        return AMDCounter;
    }

    /**
     * Returns the number of elements n added in the AMS bloom filter for a given
     * state
     *
     * @param state the given state
     *
     * @return n
     */
    public int getAMSCounter(State state) {
        return ((Integer)getAMSCounters().get(state.getStateId())).intValue();
    }

    /**
     * Gets the average number of elements added in all the AMS bloom filters
     *
     * @return the average number of elements in the AMS bloom filters
     */
    public double getAMSAverageCounters(){
        HashMap<UUID, Integer> counters=this.getAMSCounters();
        double amsSumCounters=0.0;
        for(Integer counter:counters.values()){
            amsSumCounters+=counter.doubleValue();
        }
        return amsSumCounters/counters.size();
    }

    /**
     * @return the AMSCounters
     */
    public HashMap<UUID, Integer> getAMSCounters() {
        return AMSCounters;
    }

    /**
     * Returns the possible states of the disseminator
     *
     * @param the set of possible states
     */
    public HashSet<State> getPossibleStates() {
        return possibleStates;
    }

    /**
     * Clears the existing possible states, all the bloom filters and their
     * counters. It adds the new states and creates the new bloom filters and
     * the new counters
     *
     * @param states the set with the new possible states
     */
    public void setPossibleStates(Collection<State> states){
        this.possibleStates.clear();
        this.AMSs.clear();
        this.getAMSCounters().clear();
        this.AMD.clear();
        this.AMDCounter=0;
        this.possibleStates.addAll(states);
        this.createAMSs();
        Iterator<State> it=this.possibleStates.iterator();
        while(it.hasNext()){
            this.getAMSCounters().put(it.next().getStateId(), 0);
        }
    }

    /**
     * Calculates the false positive probability given a bloom filter with a
     * certian number of items.
     *
     * @param filter a bloom filter
     * @param n the nummer of elemets added in the bloom filter
     */
    public double getFalsePositiveProbability(IFilter filter, int n){
        double k=filter.hash().k();
        double m=filter.container().size();
        double p=Math.pow((1-Math.pow(Math.E, -k*n/m)), k);
        return p;
    }

    /**
     * Calculates the false positive probabilities for every AMS bloom filter
     *
     * @return a map of the false positive probailities fom every AMS bloom filter
     * of the states
    */
    public HashMap<UUID, Double> getAMSFalsePositiveProbabilities(){
        HashMap<UUID, Double> fp=new HashMap<UUID, Double>();
        Iterator<State> it=this.AMSs.keySet().iterator();
        while(it.hasNext()){
            State possibleState=it.next();
            int n=((Integer)this.getAMSCounters().get(possibleState.getStateId())).intValue();
            double p=0.0;
            switch(this.getBloomFilterType()){
                case SIMPLE:
                    CBloomFilter bf=(CBloomFilter)this.AMSs.get(possibleState);
                    p=this.getFalsePositiveProbability(bf, n);
                    break;
                case COUNTING:
                    CCountingFilter cf=(CCountingFilter)this.AMSs.get(possibleState);
                    p=this.getFalsePositiveProbability(cf, n);
                    break;
                default:
                    // other type of bloom filter
            }
            fp.put(possibleState.getStateId(), p);
        }
        return fp;
    }

    /**
     * Provides the average false positive probabilities for all AMS bloom filters
     *
     * @return the average false positive probability for the AMS bloom filters
    */
    public double getAMSAverageFalsePositiveProbabilities(){
        HashMap<UUID, Double> fps=this.getAMSFalsePositiveProbabilities();
        double sum=0.0;
        for(Double fp:fps.values()){
            sum+=fp;
        }
        return sum/fps.size();
    }

     /**
     * Calculates the false positive probabilities for the AMD bloom filter
     *
     * @return the false positive probability of the AMD bloom filter
     */
    public double getAMDFalsePositiveProbability(){
        return this.getFalsePositiveProbability(AMD, AMDCounter);
    }

    /**
     * Checks if there is an aggregation performed before given an aggregator
     *
     * @return true if the AMD bloom filter contains a hashed item
     */
    public boolean checkAMDMembership(Finger aggregator){
        return this.AMD.contains(aggregator.toString());
    }

    /**
     * Checks if there is a given state has been selected by given aggregator
     *
     * @return true if the AMS bloom filter of the given possible state has a
     * membership of the given aggregator
     */
    public boolean checkAMSMembership(State state, Finger aggregator){
        switch(this.getBloomFilterType()){
            case SIMPLE:
                CBloomFilter bf=(CBloomFilter)this.AMSs.get(state);
                if(bf.contains(aggregator.toString())){
                    return true;
                }
                return false;
            case COUNTING:
                CCountingFilter cf=(CCountingFilter)this.AMSs.get(state);
                if(cf.contains(aggregator.toString())){
                    return true;
                }
                return false;
            default:
                return false;
                // other type of bloom filter
        }
    }

    /**
     * Creates a dissemination report that is sent to an aggregator. The
     * dissemination report contains the following:
     *
     * 1. The selected state of the disseminator
     * 2. The states that have positive memberships of the aggregator in the AMS
     * bloom filter
     * 3. The false positive probabilities of the respective states from 3
     * 4. The membership or not of the aggregator in the AMD bloom filter
     * 5. The false positives of the AMD bloom filter
     *
     * @param aggregator the aggregator in which the disseminator report is sent.
     *
     * @return the hash map representing the dissemination report
     */
    public HashMap<DisseminatorReport, Object> createDisseminatorReport(Finger aggregator){
        HashMap<DisseminatorReport, Object> report=new HashMap<DisseminatorReport, Object>();
        report.put(DisseminatorReport.SELECTED_STATE, this.selectedState);
        HashSet<State> posAMS=new HashSet<State>();
        HashMap<UUID, Double> posAMSFalsePos=new HashMap<UUID, Double>();
        Iterator<State> it=AMSs.keySet().iterator();
        while(it.hasNext()){
            State state=it.next();
            switch(this.getBloomFilterType()){
                case SIMPLE:
                    CBloomFilter bf=(CBloomFilter)this.AMSs.get(state);
                    if(bf.contains(aggregator.toString())){
                        posAMS.add(state);
                        int n=this.getAMSCounter(state);
                        double fp=this.getFalsePositiveProbability(bf, n);
                        posAMSFalsePos.put(state.getStateId(), fp);
                    }
                    break;
                case COUNTING:
                    CCountingFilter cf=(CCountingFilter)this.AMSs.get(state);;
                    if(cf.contains(aggregator.toString())){
                        posAMS.add(state);
                        int n=this.getAMSCounter(state);
                        double fp=this.getFalsePositiveProbability(cf, n);
                        posAMSFalsePos.put(state.getStateId(), fp);
                    }
                    break;
                default:
                    // other type of bloom filter
            }
        }
        report.put(DisseminatorReport.POSITIVE_AMS, posAMS);
        report.put(DisseminatorReport.POSITIVE_AMS_FP, posAMSFalsePos);
        report.put(DisseminatorReport.POSITIVE_AMD, this.AMD.contains(aggregator.toString()));
        report.put(DisseminatorReport.AMD_FP, this.getFalsePositiveProbability(AMD, AMDCounter));
        return report;
    }

    /**
     * Handles incoming reports from aggregators. An aggregation report informs
     * the disseminator if the aggregation was successful and provides (i) the AMS
     * membership removals and (ii) the AMS membership additions.
     *
     * @param aggregator the aggregator from which the report comes from
     * @param report the aggregator report
     *
     * @return tue is aggregation is acknowldged
     */
    public boolean receiveAggregatorReport(Finger aggregator, HashMap<AggregatorReport, Object> report){
        boolean ack=((Boolean)report.get(AggregatorReport.ACK)).booleanValue();
        if(ack){
            this.addAMDMembership(aggregator);
            State AMSRemoval=(State)report.get(AggregatorReport.AMS_REMOVAL);
            if(AMSRemoval!=null){
                this.removeAMSMembership(AMSRemoval, aggregator);
            }
            State AMSAddition=(State)report.get(AggregatorReport.AMS_ADDITION);
            if(AMSAddition!=null){
                this.addAMSMemebership(AMSAddition, aggregator);
            }
        }
        return ack;
    }
}
