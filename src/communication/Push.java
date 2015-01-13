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

import consistency.DisseminatorReport;
import java.util.HashMap;

/**
 * The push message is the initiator of an aggregation. It is sent by the
 * disseminator and contains the disseminator report. A push must trigger a pull
 * message.
 *
 * @author Evangelos
 */
public class Push extends DIASMessage{

    public HashMap<DisseminatorReport, Object> report;

    public Push(int session){
        this.type=DIASMessType.PUSH;
        this.aggregationEpoch=session;
    }
}
