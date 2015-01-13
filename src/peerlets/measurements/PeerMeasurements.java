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
package peerlets.measurements;

import java.util.HashMap;
import java.util.UUID;
import protopeer.Finger;

/**
 * Used for the DIAS measurements
 *
 * @author Evangelos
 */
public class PeerMeasurements {

    private Finger peer;
    private int aggregationEpoch;
    private HashMap<UUID, Integer> AMSCounters;
    private HashMap<UUID, Double> AMSFP;

    public PeerMeasurements(Finger peer){
       this.peer=peer;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PeerMeasurements other = (PeerMeasurements) obj;
        if (this.peer != other.peer && (this.peer == null || !this.peer.equals(other.peer))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.peer != null ? this.peer.hashCode() : 0);
        return hash;
    }

    /**
     * @return the peer
     */
    public Finger getPeer() {
        return peer;
    }

    /**
     * @param peer the peer to set
     */
    public void setPeer(Finger peer) {
        this.peer = peer;
    }

    /**
     * @return the aggregationEpoch
     */
    public int getSession() {
        return aggregationEpoch;
    }

    /**
     * @param aggregationEpoch the aggregationEpoch to set
     */
    public void setSession(int session) {
        this.aggregationEpoch = session;
    }

    /**
     * @return the AMSCounters
     */
    public HashMap<UUID, Integer> getAMSCounters() {
        return AMSCounters;
    }

    /**
     * @param AMSCounters the AMSCounters to set
     */
    public void setAMSCounters(HashMap<UUID, Integer> AMSCounters) {
        this.AMSCounters = AMSCounters;
    }

    /**
     * @return the AMSFP
     */
    public HashMap<UUID, Double> getAMSFP() {
        return AMSFP;
    }

    /**
     * @param AMSFP the AMSFP to set
     */
    public void setAMSFP(HashMap<UUID, Double> AMSFP) {
        this.AMSFP = AMSFP;
    }
}
