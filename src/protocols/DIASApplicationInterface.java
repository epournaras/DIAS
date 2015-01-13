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

import java.util.Collection;
import dsutil.generic.state.State;

/**
 * The DIAS Application interface defines the dynamis of the application and the
 * coupling with the DIAS aggregation. An application instance generates some
 * possible states and selects one of them. The selection can change over time. 
 *
 * @author Evangelos
 */
public interface DIASApplicationInterface {

    public Collection<State> getPossibleStates();

    public State getSelectedState();

    public Collection<State> generatePossibleStates();

    public State selectPossibleState();

    public boolean changeSelectedState();

}
