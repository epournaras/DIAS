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

/**
 * The pull-push message is a pull message that contains a push message. It is
 * used for perforing a two-way aggregation and saving one message exchange.
 *
 * @author Evangelos
 */
public class PullPush extends Pull{

    public Push push;

    public PullPush(int session){
        super(session);
        this.type=DIASMessType.PULL_PUSH;
    }
}
