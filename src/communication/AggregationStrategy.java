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
package communication;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.LinkedBlockingQueue;
import protopeer.Finger;

/**
 * The aggregation strategy provides better exploration of neighbors for improving
 * aggregation. It takes advantage of the AMD and AMS bloom filters. It keeps
 * three queues of neighbors: 
 * 
 * 1. Unexploited neighbors: Aggregation has not performed yet with them
 * 2. Exploited neighbors: They have aggregated the earliest selected state
 * 3. Outdated neighbors: They have aggregated an older selected state
 * 
 * Based on this available neighbors, aggregation is performed based on three
 * strategies:
 * 
 * 1. RANDOM: Unexploited or oudated
 * 2. EXPLOITATION: Unexploited else outdated
 * 3. UPDATE" Outdated else unexploined
 * 
 * In all the three strategies, if one of the lists is empty, a neighbor is
 * returned from the other one respectively. 
 * 
 * When a change in the selected state is performed, the neighbors are rearranged 
 * and adapted appropriatelly.
 *
 *
 * @author Evangelos
 */
public class AggregationStrategy {

    public enum Strategy{
        RANDOM,
        EXPLOITATION,
        UPDATE,
   }

    private Strategy strategy;
    private LinkedBlockingQueue<Finger> unexploitedNeighbors;
    private LinkedBlockingQueue<Finger> outdatedNeighbors;
    private LinkedBlockingQueue<Finger> exploitedNeighbors;
    
    /**
     * Initialization
     *
     * @param strategy the selected strategy, RANDOM, EXPLOITATION or UPDATE
     * @param unexploitedSize the maximum number of neighbors in the queue with
     * the unexploited neighbors
     * @param outdatedSize the maximum number of neighbors in the queue with the
     * outdated neighbors
     * @param exploitedSize the maximum number of neighbors in the queue with the
     * exploited neighbors
     */
    public AggregationStrategy(Strategy strategy, int unexploitedSize, int outdatedSize, int exploitedSize){
        this.strategy=strategy;
        this.unexploitedNeighbors=new LinkedBlockingQueue<Finger>(unexploitedSize);
        this.outdatedNeighbors=new LinkedBlockingQueue<Finger>(outdatedSize);
        this.exploitedNeighbors=new LinkedBlockingQueue<Finger>(exploitedSize);
    }

    /**
     * Inserts a sampled neighbor in the appropriate queue based on the checks
     * of the AMD and AMS bloom filters
     *
     * @param sample the sample inserted
     * @param amdCheck the result of the check in the AMD bloom filter
     * @param amsCheck the result of the check in the AMS bloom filter
     */
    public boolean setSample(Finger sample, boolean amdCheck, boolean amsCheck){
        if(amdCheck==false){
            return this.unexploitedNeighbors.offer(sample);
        }
        else{
            if(amsCheck==false){
                return this.outdatedNeighbors.offer(sample);
            }
            else{
                return this.exploitedNeighbors.offer(sample);
            }
        }
    }

    /**
     * Returns a neighbor according to the selected strategy.
     *
     * @return the selected finger neighbor
     */
    public Finger getSample(){
        Finger sample=null;
        switch(strategy){
            case RANDOM:
                double seed=Math.random();
                if(seed>0.5){
                    sample=this.unexploitedNeighbors.poll();
                    if(sample==null){
                        sample=this.outdatedNeighbors.poll();
                    }
                    return sample;
                }
                else{
                    sample=this.outdatedNeighbors.poll();
                    if(sample==null){
                        sample=this.unexploitedNeighbors.poll();
                    }
                    return sample;
                }
            case EXPLOITATION:
                sample=this.unexploitedNeighbors.poll();
                if(sample==null){
                    sample=this.outdatedNeighbors.poll();
                }
                return sample;
            case UPDATE:
                sample=this.outdatedNeighbors.poll();
                if(sample==null){
                    sample=this.unexploitedNeighbors.poll();
                }
                return sample;
            default:
                return sample;
                // do something else
        }
    }

    /**
     * This method is called when a selected state changes. It performs the
     * following:
     *
     * 1. Removes and exports the outdated neighbors
     * 2. Exploited neighbors become outdated
     *
     * @return the outdated neighbors for checking if the are exploited or not
     */
    public Collection<Finger> exportOutdated(){
        LinkedHashSet<Finger> outdatedBuffer=new LinkedHashSet<Finger>();
        this.outdatedNeighbors.drainTo(outdatedBuffer);
        this.exploitedNeighbors.drainTo(this.outdatedNeighbors);
        return outdatedBuffer;
    }

    /**
     * Inserts an exploited neighbor after a a rearrangement triggered by a change
     * in the selected state
     */
    public void importExploited(Finger exploited){
        this.exploitedNeighbors.offer(exploited);
    }

    /**
     * Inserts an outdated neighbor after a a rearrangement triggered by a change
     * in the selected state
     */
    public void importOutdated(Finger outdated){
        this.outdatedNeighbors.offer(outdated);
    }

    /**
     * @param strategy the strategy to set
     */
    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Checks if an aggregation is possible given the checks in the AMD and AMS
     * bloom filters
     *
     * @param amdCheck the resulf of the check in the AMD bloom filter
     * @param amsCheck the result of the check in the AMS bloom filter
     *
     * @return true if an aggregation is indicated with an unexploited or outdated
     * neighbor
     */
    public boolean isPossibleAggregation(boolean amdCheck, boolean amsCheck){
        if(amdCheck==false){
            return true;
        }
        else{
            if(amsCheck==false){
                return true;
            }
            return false;
        }
    }

    /**
     * Removes a neighbor if contained in one of the queues.
     *
     * @param sampe the removed neighbor
     *
     * @return true if the neighbor is removed
     */
    public boolean removeNeighbor(Finger sample){
        if(this.containsSample(sample)){
            this.outdatedNeighbors.remove(sample);
            this.exploitedNeighbors.remove(sample);
            this.unexploitedNeighbors.remove(sample);
            return true;
        }
        return false;
    }

    /**
     * @param sample the checked neighbor
     *
     * @return true if the checked neighbor is one of the queues of the strategy
     */
    public boolean containsSample(Finger sample){
        if(this.unexploitedNeighbors.contains(sample)||
                this.outdatedNeighbors.contains(sample)||
                this.exploitedNeighbors.contains(sample)){
            return true;
        }
        return false;
    }

    /**
     * Removes all the elements from the queues.
     */
    public void clear(){
        this.unexploitedNeighbors.clear();
        this.outdatedNeighbors.clear();
        this.exploitedNeighbors.clear();
    }

    public int getOutdatedSize(){
        return this.outdatedNeighbors.size();
    }

    public int getExploitedSize(){
        return this.exploitedNeighbors.size();
    }

    public int getUnexploitedSize(){
        return this.unexploitedNeighbors.size();
    }
}
