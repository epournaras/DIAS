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

import consistency.AggregatorReport;
import java.util.HashMap;

/**
 * The pull message is the reply to a push message. A pull contains the aggregator
 * report with the result of the aggregation. 
 *
 * @author Evangelos
 */
public class Pull extends DIASMessage{

    public HashMap<AggregatorReport, Object> report;

    public Pull(int session){
        this.type=DIASMessType.PULL;
        this.aggregationEpoch=session;
    }
}
