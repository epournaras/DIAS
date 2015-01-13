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
package peerlets;

import dsutil.protopeer.services.aggregation.AggregationFunction;
import dsutil.protopeer.services.aggregation.AggregationType;
import java.util.Collection;
import dsutil.generic.state.State;

/**
 * The interface of the aggregation framework DIAS: Dynamic Intelligent Aggregation
 * Service.
 * 
 * It defines the services provided to the applications that use it.
 *
 * @author Evangelos
 */
public interface DIASInterface {
    
//    public boolean isActive();
    
//    public Object getAggregate(AggregationFunction function);
//
//    public void requestAggregation(AggregationType type, Collection<State> states, State selectedState);
//
//    public void changeSelectedState(State state);
    
    public void collectSamples();

}
