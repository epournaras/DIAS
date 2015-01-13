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
package aggregation;

import dsutil.protopeer.services.aggregation.AggregationType;
import dsutil.protopeer.services.aggregation.AggregationFunction;
import dsutil.generic.state.ArithmeticListState;
import dsutil.generic.state.ArithmeticState;
import dsutil.generic.state.State;
import dsutil.generic.state.StateException;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregation state keep information about the aggregation of two types of
 * states: (i) arithmetic states and (ii) arithmetic list states. The arithmetic
 * state concerns a simple double value whereas and arithmetic list state concern
 * a list of arithmeti states. An aggregation state assumes that the aggregation
 * list states have the same number of arithmetic lists. This should change in
 * future. Furthermore, the aggregation functions for the arithmetic list states
 * always output an arithmetic list state with the same number of arithmetic
 * states. Aggregation between the arithmetic states of a single arithmetic list
 * states is not supported for now and it is left for the application. 
 * 
 * The supported aggregation functions are the following:
 * 
 * SUM, MAX, MIN, AVG, STDEV, COUNT, SUM_SQR
 * 
 * Note that, MAX and MIN are double-insensitive aggregation functions. However,
 * the rest of them are double-sensitive.
 * 
 * States can be added and removed from the aggregation state. However, a removal
 * may cause to MAX and MIN inconsistencies. Consistency is left to be
 * implemented by the application. However, this class supports the update of
 * the MAX and MIN states by previosly added states. Because these aggregation
 * functions are double-insensitive, the inconsistencies can be resolved in this
 * way.
 *
 * @author Evangelos
 */
public class AggregationState extends State{

    private AggregationType aggregationType;

    /**
     * Initializes an aggregation state by providing the aggregation type. In
     * case of a <code>Type</code> about an <code>ArithmeticState</code> the
     * aggregates are initialized with NaN. In the case of an
     * <code>ArithmeticListState</code> the list of arithmetic states is empty.
     * 
     * @param selectedType the selected type of the aggregation state
     */
    public AggregationState(AggregationType selectedType){
        super();
        this.initAggregates(selectedType);
    }

    /**
     * Initializes an aggregation state with a given state, arithmetic or
     * arithmetic list. 
     *
     * @param state a state for initialization.
     */
    public AggregationState(State state){
        super();
        try{
            this.initAggregates(state);
        }
        catch(StateException ex){
            System.out.println(ex.toString()+ex.getStateExcMsg());
        }
    }

    /**
     * Adds a state in the aggregates. The supported types of states are (i) the
     * arithmetic and (ii) the arithmetic list. The states must have been
     * initialized (contains a value in arithmetic states or a non-empty list in
     * arithmetic list states).
     *
     * @param state an added state
     */
    public void addState(State state) throws StateException{
        if(state instanceof ArithmeticState && aggregationType.equals(AggregationType.ARITHMETIC)){
            ArithmeticState addState=(ArithmeticState)state;
            if(addState.containsValue()){
                if(this.properties.size()==0){
                    throw new StateException("Arithmetic state cannot be added. Aggregates have not been initialized", state);
                }
                else{
                    this.addArithmeticState(addState);
                }
            }
            else{
                throw new StateException("Arithmetic state added does not contain a value", state);
            }
        }
        else{
            if(state instanceof ArithmeticListState && aggregationType.equals(AggregationType.ARITHMETIC_LIST)){
                ArithmeticListState addState=(ArithmeticListState)state;
                if(addState.containsArithmeticList()){
                    if(this.properties.size()==0){
                        throw new StateException("Arithmetic list state cannot be added. Aggregates have not been initialized", state);
                    }
                    else{
                        this.addArithmeticListState(addState);
                    }
                }
                else{
                    throw new StateException("Arithmetic list state added does not contain arithmetic states", state);
                }
            }
            else{
                throw new StateException("Invalid state type", state);
            }
        }
    }
    
    /**
     * Adds a new arithmetic state by recomputing the aggregates. The AVERAGE and
     * the STDEV are calculated based on the SUM and COUNT aggregates. The
     * aggregates are also checked if they are initialized before they are
     * updated. 
     * 
     * @param addState the added arithmetic state
     */
    private void addArithmeticState(ArithmeticState addState){
        // SUM
        ArithmeticState sumState=(ArithmeticState)this.getProperty(AggregationFunction.SUM);
        if(sumState.containsValue()){
            sumState.setValue(sumState.getValue()+addState.getValue());
        }
        else{
            sumState.setValue(addState.getValue());
        }
        // SUM_SQR
        ArithmeticState sumSquareState=(ArithmeticState)this.getProperty(AggregationFunction.SUM_SQR);
        if(sumSquareState.containsValue()){
            sumSquareState.setValue(sumSquareState.getValue()+Math.pow(addState.getValue(),2));
        }
        else{
            sumSquareState.setValue(Math.pow(addState.getValue(),2));
        }
        // MAX
        ArithmeticState maxState=(ArithmeticState)this.getProperty(AggregationFunction.MAX);
        if(maxState.containsValue()){
            maxState.setValue(Math.max(maxState.getValue(), addState.getValue()));
        }
        else{
            maxState.setValue(addState.getValue());
        }
        // MIN
        ArithmeticState minState=(ArithmeticState)this.getProperty(AggregationFunction.MIN);
        if(minState.containsValue()){
            minState.setValue(Math.min(minState.getValue(), addState.getValue()));
        }
        else{
            minState.setValue(addState.getValue());
        }
        // COUNT
        ArithmeticState countState=(ArithmeticState)this.getProperty(AggregationFunction.COUNT);
        if(countState.containsValue()){
            countState.setValue(countState.getValue()+1.0);
        }
        else{
            countState.setValue(1.0);
        }
        // AVG
        ArithmeticState avgState=(ArithmeticState)this.getProperty(AggregationFunction.AVG);
        avgState.setValue(sumState.getValue()/countState.getValue());
        //STDEV - σ=E[X^2]-E[X] or SQRT(VARIANCE) where VARIANCE=1/(n-1)(Σx^2-1/n(Σx)^2)
        ArithmeticState stdevState=(ArithmeticState)this.getProperty(AggregationFunction.STDEV);
        stdevState.setValue(Math.sqrt((sumSquareState.getValue()/countState.getValue()-(Math.pow(sumState.getValue()/countState.getValue(), 2)))));
    }
    
    /**
     * Adds a new arithmetic list state by recomputing the aggregates. The 
     * AVERAGE and the STDEV are calculated based on the SUM and COUNT aggregates.
     * The aggregates are also checked if they are initialized before they are
     * updated. 
     * 
     * @param addState the added arithmetic list state
     */
    private void addArithmeticListState(ArithmeticListState addState){
        // SUM
        ArithmeticListState sumArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.SUM);
        int index=0;
        if(sumArithmeticList.containsArithmeticList()){
            for(ArithmeticState as:addState.getArithmeticStates()){
                double newSum=as.getValue()+sumArithmeticList.getArithmeticState(index).getValue();
                sumArithmeticList.setArithmeticState(index, newSum);
                index++;
            }
        }
        else{
            for(ArithmeticState as:addState.getArithmeticStates()){
                ArithmeticState newSum=new ArithmeticState(as.getValue());
                sumArithmeticList.addArithmeticState(newSum);
            }
        }
        // SUM_SQR
        ArithmeticListState sumSquareArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.SUM_SQR);
        index=0;
        if(sumSquareArithmeticList.containsArithmeticList()){
            for(ArithmeticState as:addState.getArithmeticStates()){
                double newSumSquare=Math.pow(as.getValue(), 2)+sumSquareArithmeticList.getArithmeticState(index).getValue();
                sumSquareArithmeticList.setArithmeticState(index, newSumSquare);
                index++;
            }
        }
        else{
            for(ArithmeticState as:addState.getArithmeticStates()){
                ArithmeticState newSumSquare=new ArithmeticState(Math.pow(as.getValue(), 2));
                sumSquareArithmeticList.addArithmeticState(newSumSquare);
            }
        }
        // MAX
        ArithmeticListState maxArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.MAX);
        index=0;
        if(maxArithmeticList.containsArithmeticList()){
            for(ArithmeticState as:addState.getArithmeticStates()){
                double newMax=Math.max(as.getValue(), maxArithmeticList.getArithmeticState(index).getValue());
                maxArithmeticList.setArithmeticState(index, newMax);
                index++;
            }
        }
        else{
            for(ArithmeticState as:addState.getArithmeticStates()){
                ArithmeticState newMax=new ArithmeticState(as.getValue());
                maxArithmeticList.addArithmeticState(newMax);
            }
        }
        // MIN
        ArithmeticListState minArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.MIN);
        index=0;
        if(minArithmeticList.containsArithmeticList()){
            for(ArithmeticState as:addState.getArithmeticStates()){
                double newMin=Math.min(as.getValue(), minArithmeticList.getArithmeticState(index).getValue());
                minArithmeticList.setArithmeticState(index, newMin);
                index++;
            }
        }
        else{
            for(ArithmeticState as:addState.getArithmeticStates()){
                ArithmeticState newMin=new ArithmeticState(as.getValue());
                minArithmeticList.addArithmeticState(newMin);
            }
        }
        // COUNT
        ArithmeticListState countArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.COUNT);
        index=0;
        if(countArithmeticList.containsArithmeticList()){
            for(ArithmeticState as:addState.getArithmeticStates()){
                double newCount=countArithmeticList.getArithmeticState(index).getValue()+1.0;
                countArithmeticList.setArithmeticState(index, newCount);
                index++;
            }
        }
        else{
            for(ArithmeticState as:addState.getArithmeticStates()){
                ArithmeticState newCount=new ArithmeticState(1.0);
                countArithmeticList.addArithmeticState(newCount);
            }
        }
        // AVG
        ArithmeticListState avgArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.AVG);
        index=0;
        if(avgArithmeticList.containsArithmeticList()){
            for(ArithmeticState as:addState.getArithmeticStates()){
                double newAvg=sumArithmeticList.getArithmeticState(index).getValue()/countArithmeticList.getArithmeticState(index).getValue();
                avgArithmeticList.setArithmeticState(index, newAvg);
                index++;
            }
        }
        else{
            for(ArithmeticState as:addState.getArithmeticStates()){
                ArithmeticState newAvg=new ArithmeticState(as.getValue());
                avgArithmeticList.addArithmeticState(newAvg);
            }
        }
        // STDEV - σ=E[X^2]-E[X] or SQRT(VARIANCE) where VARIANCE=1/(n-1)(Σx^2-1/n(Σx)^2)
        ArithmeticListState stdevArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.STDEV);
        index=0;
        if(stdevArithmeticList.containsArithmeticList()){
            for(ArithmeticState as:addState.getArithmeticStates()){
                double newStdev=Math.sqrt((sumSquareArithmeticList.getArithmeticState(index).getValue()/countArithmeticList.getArithmeticState(index).getValue()-
                        (Math.pow(sumArithmeticList.getArithmeticState(index).getValue()/countArithmeticList.getArithmeticState(index).getValue(), 2))));
                stdevArithmeticList.setArithmeticState(index, newStdev);
                index++;
            }
        }
        else{
            for(ArithmeticState as:addState.getArithmeticStates()){
                ArithmeticState newStdev=new ArithmeticState(0.0);
                stdevArithmeticList.addArithmeticState(newStdev);
            }
        }
        
    }

    /**
     * Removes a state in the aggregates. The supported types of states are (i) the
     * arithmetic and (ii) the arithmetic list. The states must have been
     * initialized (contains a value in arithmetic states or a non-empty list in
     * arithmetic list states).
     *
     * @param state a removed state
     */
    public void removeState(State state) throws StateException{
        if(state instanceof ArithmeticState && aggregationType.equals(AggregationType.ARITHMETIC)){
            ArithmeticState removeState=(ArithmeticState)state;
            if(removeState.containsValue()){
                if(this.properties.size()==0){
                    throw new StateException("Arithmetic state cannot be removed. Aggregates have not been initialized", state);
                }
                else{
                    this.removeArithmeticState(removeState);
                }
            }
            else{
                throw new StateException("Arithmetic state removed does not contain a value", state);
            }
        }
        else{
            if(state instanceof ArithmeticListState && aggregationType.equals(AggregationType.ARITHMETIC_LIST)){
                ArithmeticListState removeState=(ArithmeticListState)state;
                if(removeState.containsArithmeticList()){
                    if(this.properties.size()==0){
                        throw new StateException("Arithmetic state cannot be added. Aggregates have not been initialized", state);
                    }
                    else{
                        this.removeArithmeticListState(removeState);
                    }
                }
                else{
                    throw new StateException("Arithmetic list state removed does not contain arithmetic states", state);
                }
            }
            else{
                throw new StateException("Invalid state", state);
            }
        }
    }
    
    /**
     * Removes an arithmetic state by recomputing the aggregates. The AVERAGE and
     * the STDEV are calculated based on the SUM and COUNT aggregates. The
     * aggregates are also checked if they are initialized before they are
     * updated. The removal assumes that the removed state does exist in the
     * aggregates. 
     * 
     * @param removeState the removed arithmetic state
     */
    private void removeArithmeticState(ArithmeticState removeState){
        // SUM
        ArithmeticState sumState=(ArithmeticState)this.getProperty(AggregationFunction.SUM);
        sumState.setValue(sumState.getValue()-removeState.getValue());
        // SUM_SQR
        ArithmeticState sumSquareState=(ArithmeticState)this.getProperty(AggregationFunction.SUM_SQR);
        sumSquareState.setValue(sumSquareState.getValue()-Math.pow(removeState.getValue(),2));
        // MAX
        ArithmeticState maxState=(ArithmeticState)this.getProperty(AggregationFunction.MAX);
        if(maxState.getValue()==removeState.getValue()){
            maxState.setValue(Double.MIN_VALUE);
        }
        // MIN
        ArithmeticState minState=(ArithmeticState)this.getProperty(AggregationFunction.MIN);
        if(minState.getValue()==removeState.getValue()){
            minState.setValue(Double.MAX_VALUE);
        }
        // COUNT
        ArithmeticState countState=(ArithmeticState)this.getProperty(AggregationFunction.COUNT);
        countState.setValue(countState.getValue()-1.0);
        // AVG
        ArithmeticState avgState=(ArithmeticState)this.getProperty(AggregationFunction.AVG);
        avgState.setValue(sumState.getValue()/countState.getValue());
        // STDEV - σ=E[X^2]-E[X] or SQRT(VARIANCE) where VARIANCE=1/(n-1)(Σx^2-1/n(Σx)^2)
        ArithmeticState stdevState=(ArithmeticState)this.getProperty(AggregationFunction.STDEV);
        stdevState.setValue(Math.sqrt((sumSquareState.getValue()/countState.getValue()-(Math.pow(sumState.getValue()/countState.getValue(), 2)))));
    }

    /**
     * Removes an arithmetic list state by recomputing the aggregates. The
     * AVERAGE and the STDEV are calculated based on the SUM and COUNT
     * aggregates. The aggregates are also checked if they are initialized before
     * they are updated. The removal assumes that the removed state does exist
     * in the aggregates. 
     * 
     * @param removeState the removed arithmetic list state
     */
    private void removeArithmeticListState(ArithmeticListState removeState){
        // SUM
        ArithmeticListState sumArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.SUM);
        int index=0;
        for(ArithmeticState as:removeState.getArithmeticStates()){
            double newSum=sumArithmeticList.getArithmeticState(index).getValue()-as.getValue();
            sumArithmeticList.setArithmeticState(index, newSum);
            index++;
        }
        // SUM_SQR
        ArithmeticListState sumSquareArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.SUM_SQR);
        index=0;
        for(ArithmeticState as:removeState.getArithmeticStates()){
            double newSumSquare=sumSquareArithmeticList.getArithmeticState(index).getValue()-Math.pow(as.getValue(), 2);
            sumSquareArithmeticList.setArithmeticState(index, newSumSquare);
            index++;
        }
        // MAX
        ArithmeticListState maxArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.MAX);
        index=0;
        for(ArithmeticState as:removeState.getArithmeticStates()){
            if(as.getValue()==maxArithmeticList.getArithmeticState(index).getValue()){
                maxArithmeticList.setArithmeticState(index, Double.MIN_VALUE);
            }
            index++;
        }
        // MIN
        ArithmeticListState minArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.MIN);
        index=0;
        for(ArithmeticState as:removeState.getArithmeticStates()){
            if(as.getValue()==minArithmeticList.getArithmeticState(index).getValue()){
                minArithmeticList.setArithmeticState(index, Double.MAX_VALUE);
            }
            index++;
        }
        // COUNT
        ArithmeticListState countArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.COUNT);
        index=0;
        for(ArithmeticState as:removeState.getArithmeticStates()){
            double newCount=countArithmeticList.getArithmeticState(index).getValue()-1.0;
            countArithmeticList.setArithmeticState(index, newCount);
            index++;
        }
        // AVG
        ArithmeticListState avgArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.AVG);
        index=0;
        for(ArithmeticState as:removeState.getArithmeticStates()){
            double newAvg=sumArithmeticList.getArithmeticState(index).getValue()/countArithmeticList.getArithmeticState(index).getValue();
            avgArithmeticList.setArithmeticState(index, newAvg);
            index++;
        }
        // STDEV - σ=E[X^2]-E[X] or SQRT(VARIANCE) where VARIANCE=1/(n-1)(Σx^2-1/n(Σx)^2)
        ArithmeticListState stdevArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.STDEV);
        index=0;
        for(ArithmeticState as:removeState.getArithmeticStates()){
            double newStdev=Math.sqrt((sumSquareArithmeticList.getArithmeticState(index).getValue()/countArithmeticList.getArithmeticState(index).getValue()-
                    (Math.pow(sumArithmeticList.getArithmeticState(index).getValue()/countArithmeticList.getArithmeticState(index).getValue(), 2))));
            stdevArithmeticList.setArithmeticState(index, newStdev);
            index++;
        }
    }
    
    /**
     * Updates the MAX and MIN aggregation functions with a state for . The purpose
     * of this method is to capture the removals of double-insensitive aggregation
     * functions. More specifically, a state that is already contained in some
     * of the aggregates, e.g. sum, can be used to update MAX and MIN only 
     * instead of adding again the same state and introducing duplicates in
     * aggregation functions such as MAX and MIN. 
     *
     * @param state a state that updates the MAX and MIN aggregation functions
     */
    public void updateMaxMin(State state) throws StateException{
        if(state instanceof ArithmeticState){
            ArithmeticState updateState=(ArithmeticState)state;
            if(updateState.containsValue()){
                if(this.properties.size()==0){
                    this.initAggregates((ArithmeticState)state);
                }
                else{
                    // MAX
                    ArithmeticState maxState=(ArithmeticState)this.getProperty(AggregationFunction.MAX);
                    maxState.setValue(Math.max(maxState.getValue(), updateState.getValue()));
                    // MIN
                    ArithmeticState minState=(ArithmeticState)this.getProperty(AggregationFunction.MIN);
                    minState.setValue(Math.min(minState.getValue(), updateState.getValue()));
                }
            }
            else{
                throw new StateException("Arithmetic state does not contain a value", state);
            }
        }
        else{
            if(state instanceof ArithmeticListState){
                ArithmeticListState updateState=(ArithmeticListState)state;
                int index=0;
                if(updateState.containsArithmeticList()){
                    if(this.properties.size()==0){
                        this.initAggregates((ArithmeticState)state);
                    }
                    else{
                        // MAX
                        ArithmeticListState maxArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.MAX);
                        index=0;
                        for(ArithmeticState as:updateState.getArithmeticStates()){
                            double newMax=Math.max(as.getValue(), maxArithmeticList.getArithmeticState(index).getValue());
                            maxArithmeticList.setArithmeticState(index, newMax);
                            index++;
                        }
                        // MIN
                        ArithmeticListState minArithmeticList=(ArithmeticListState)this.getProperty(AggregationFunction.MIN);
                        index=0;
                        for(ArithmeticState as:updateState.getArithmeticStates()){
                            double newMin=Math.min(as.getValue(), minArithmeticList.getArithmeticState(index).getValue());
                            minArithmeticList.setArithmeticState(index, newMin);
                            index++;
                        }
                    }
                }
                else{
                    throw new StateException("Arithmetic list state does not contain arithmetic states", state);
                }
            }
            else{
                throw new StateException("Invalid state", state);
            }
        }
    }

    /**
     * Initializes aggregates with a given arithmetic or arithmetic list state
     * that should also be initialized. 
     *
     * @param state an arithmetic or arithmetic lsit state given for initialization
     */
    private void initAggregates(State state) throws StateException{
        try{
            State sumState=null;
            State sumSquareState=null;
            State maxState=null;
            State minState=null;
            State avgState=null;
            State stdevState=null;
            State countState=null;
            if(state instanceof ArithmeticState){
                aggregationType=AggregationType.ARITHMETIC;
                ArithmeticState arithmeticState=(ArithmeticState)state;
                if(arithmeticState.containsValue()){
                    sumState=new ArithmeticState(arithmeticState.getValue());
                    sumSquareState=new ArithmeticState(Math.pow(arithmeticState.getValue(),2));
                    maxState=new ArithmeticState(arithmeticState.getValue());
                    minState=new ArithmeticState(arithmeticState.getValue());
                    avgState=new ArithmeticState(arithmeticState.getValue());
                    stdevState=new ArithmeticState(0.0);
                    countState=new ArithmeticState(1.0);
                }
                else{
                    throw new StateException("Arithmetic state does not contain a value", state);
                }
            }
            else{
                if(state instanceof ArithmeticListState){
                    aggregationType=AggregationType.ARITHMETIC_LIST;
                    ArithmeticListState arithmeticListState=(ArithmeticListState)state;
                    if(arithmeticListState.containsArithmeticList()){
                        sumState=new ArithmeticListState(arithmeticListState.getArithmeticStates());
                        sumSquareState=new ArithmeticListState(arithmeticListState.getArithmeticStates());
                        ArithmeticListState sumSquareListState=(ArithmeticListState)sumSquareState;
                        List<ArithmeticState> sumSquarerList=sumSquareListState.getArithmeticStates();
                        int index=0;
                        for(ArithmeticState as:sumSquarerList){
                            sumSquareListState.setArithmeticState(index, Math.pow(as.getValue(), 2));
                            index++;
                        }
                        maxState=new ArithmeticListState(arithmeticListState.getArithmeticStates());
                        minState=new ArithmeticListState(arithmeticListState.getArithmeticStates());
                        avgState=new ArithmeticListState(arithmeticListState.getArithmeticStates());
                        stdevState=new ArithmeticListState(arithmeticListState.getArithmeticStates());
                        ArithmeticListState stdevListState=(ArithmeticListState)stdevState;
                        List<ArithmeticState> stdevList=stdevListState.getArithmeticStates();
                        index=0;
                        for(ArithmeticState as:stdevList){
                            stdevListState.setArithmeticState(index, 0.0);
                            index++;
                        }
                        countState=new ArithmeticListState(arithmeticListState.getArithmeticStates());
                        ArithmeticListState countListState=(ArithmeticListState)countState;
                        List<ArithmeticState> countList=countListState.getArithmeticStates();
                        index=0;
                        for(ArithmeticState as:countList){
                            countListState.setArithmeticState(index, 1.0);
                            index++;
                        }
                    }
                    else{
                        throw new StateException("Arithmetic list state does not contain arithmetic states", state);
                    }
                }
                else{
                    throw new StateException("Invalid state", state);
                }
            }
            this.addProperty(AggregationFunction.SUM, sumState);
            this.addProperty(AggregationFunction.SUM_SQR, sumSquareState);
            this.addProperty(AggregationFunction.MAX, maxState);
            this.addProperty(AggregationFunction.MIN, minState);
            this.addProperty(AggregationFunction.AVG, avgState);
            this.addProperty(AggregationFunction.STDEV, stdevState);
            this.addProperty(AggregationFunction.COUNT, countState);
        }
        catch(StateException se){
            System.out.println(se.toString()+se.getStateExcMsg());
        }
    }

    /**
     * Initializes aggregates with NaN values in the case of an arithmetic state
     * or with an empty list in the case of an arithmetic list state.
     * 
     * @param selectedType the type of the aggregates that will be stored
     */
    private void initAggregates(AggregationType selectedType){
        this.aggregationType=selectedType;
        try{
            State sumState=null;
            State sumSquareState=null;
            State maxState=null;
            State minState=null;
            State avgState=null;
            State stdevState=null;
            State countState=null;
            if(this.aggregationType.equals(AggregationType.ARITHMETIC)){
                sumState=new ArithmeticState(Double.NaN);
                sumSquareState=new ArithmeticState(Double.NaN);
                maxState=new ArithmeticState(Double.NaN);
                minState=new ArithmeticState(Double.NaN);
                avgState=new ArithmeticState(Double.NaN);
                stdevState=new ArithmeticState(Double.NaN);
                countState=new ArithmeticState(Double.NaN);
            }
            else{
                if(this.aggregationType.equals(AggregationType.ARITHMETIC_LIST)){
                    sumState=new ArithmeticListState(new ArrayList<ArithmeticState>());
                    sumSquareState=new ArithmeticListState(new ArrayList<ArithmeticState>());
                    maxState=new ArithmeticListState(new ArrayList<ArithmeticState>());
                    minState=new ArithmeticListState(new ArrayList<ArithmeticState>());
                    avgState=new ArithmeticListState(new ArrayList<ArithmeticState>());
                    stdevState=new ArithmeticListState(new ArrayList<ArithmeticState>());
                    countState=new ArithmeticListState(new ArrayList<ArithmeticState>());
                    
                }
                else{
                    throw new StateException("Invalid type of state", null);
                }
            }
            this.addProperty(AggregationFunction.SUM, sumState);
            this.addProperty(AggregationFunction.SUM_SQR, sumSquareState);
            this.addProperty(AggregationFunction.MAX, maxState);
            this.addProperty(AggregationFunction.MIN, minState);
            this.addProperty(AggregationFunction.AVG, avgState);
            this.addProperty(AggregationFunction.STDEV, stdevState);
            this.addProperty(AggregationFunction.COUNT, countState);
        }
        catch(StateException se){
            System.out.println(se.toString()+se.getStateExcMsg());
        }
    }
    
    /**
     * Returns the aggregate object of a specific aggregation function based on
     * the selected aggregation type. 
     * 
     * @param function the aggregation function
     * @return the aggregate object
     */
    public Object getAggregate(AggregationFunction function){
        switch(this.aggregationType){
            case ARITHMETIC:
                return this.getArithmeticAggregate(function);
            case ARITHMETIC_LIST:
                return this.getArithmeticListAggregate(function);
            default: 
                return null;
        }
    }

    /**
     * Gets the value of an aggregate arithmetic state
     *
     * @param function the <code>Function</code> of aggregate
     * @return the value of the aggregate
     */
    private double getArithmeticAggregate(AggregationFunction function){
        ArithmeticState aggregate=(ArithmeticState)this.getProperty(function);
        return aggregate.getValue();
    }

    /**
     * Sets the value of an aggregate arithmetic state
     *
     * param function the <code>Function</code> of aggregate
     * @param value the arithmetic value of an aggregate
     */
    private void setArithmeticAggregate(AggregationFunction function, double value){
        ArithmeticState aggregate=(ArithmeticState)this.getProperty(function);
        aggregate.setValue(value);
    }
    
    /**
     * Gets the values of an aggregate arithmetic list state
     *
     * @param function the <code>Function</code> of aggregate
     * @return the artihmetic states of the aggregate
     */
    private List<ArithmeticState> getArithmeticListAggregate(AggregationFunction function){
        ArithmeticListState aggregate=(ArithmeticListState)this.getProperty(function);
        return aggregate.getArithmeticStates();
    }
    
    /**
     * Sets the list of arithmetic states of an aggregate arithmetic list state
     *
     * param function the <code>Function</code> of aggregate
     * @param values the arithmetic states of an aggregate
     */
    private void setArithmeticListAggregate(AggregationFunction function, List<ArithmeticState> arithmeticStates){
        ArithmeticListState aggregate=(ArithmeticListState)this.getProperty(function);
        aggregate.setArithmeticStates(arithmeticStates);
    } 

    /**
     * Removes all the aggregates and adds new ones initialized as NaN
     * 
     * @param selectedType the selected type of the initialized aggregation state
     */
    public void resetAggregationState(AggregationType selectedType){
        this.clear();
        this.initAggregates(selectedType);
    }

    /**
     * Provides the Type of aggregation configured
     * 
     * @return the Type of aggregation
     */
    public AggregationType getAggregationType(){
        return this.aggregationType;
    }
    
    /**
     * Provides information about the computed aggregates
     *
     * @return the string information
     */
    @Override
    public String toString(){
        try{
            if(this.aggregationType.equals(AggregationType.ARITHMETIC)){
                return"Aggregate ID: "+this.getStateId()+
                    "\n["+AggregationFunction.SUM.name()+"="+getArithmeticAggregate(AggregationFunction.SUM)+", "+
                  AggregationFunction.MAX.name()+"="+getArithmeticAggregate(AggregationFunction.MAX)+", "+
                  AggregationFunction.MIN.name()+"="+getArithmeticAggregate(AggregationFunction.MIN)+", "+
                  AggregationFunction.COUNT.name()+"="+getArithmeticAggregate(AggregationFunction.COUNT)+", "+
                  AggregationFunction.AVG.name()+"="+getArithmeticAggregate(AggregationFunction.AVG)+", "+
                  AggregationFunction.STDEV.name()+"="+getArithmeticAggregate(AggregationFunction.STDEV)+"]";
            }
            else{
                if(this.aggregationType.equals(AggregationType.ARITHMETIC_LIST)){
                    return"Aggregate ID: "+this.getStateId()+
                        "\n["+AggregationFunction.SUM.name()+"="+getArithmeticListAggregate(AggregationFunction.SUM)+", "+
                      AggregationFunction.SUM_SQR.name()+"="+getArithmeticListAggregate(AggregationFunction.SUM_SQR)+", "+
                      AggregationFunction.MAX.name()+"="+getArithmeticListAggregate(AggregationFunction.MAX)+", "+
                      AggregationFunction.MIN.name()+"="+getArithmeticListAggregate(AggregationFunction.MIN)+", "+
                      AggregationFunction.COUNT.name()+"="+getArithmeticListAggregate(AggregationFunction.COUNT)+", "+
                      AggregationFunction.STDEV.name()+"="+getArithmeticListAggregate(AggregationFunction.STDEV)+"]";
                }
                else{
                    throw new StateException("Invalid type of state", null);
                }
            }
        }
        catch(StateException se){
            return se.toString()+se.getStateExcMsg();
        }
    }

}