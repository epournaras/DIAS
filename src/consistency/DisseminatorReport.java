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
package consistency;

/**
 * The disseminator report contains infromation about the local states and 
 * consistency information about the aggregation. It is outlided as follows:
 * 
 * 1. SELECTED_STATE: The selected state of the disseminator provided by the
 * application.
 * 
 * 2. POSITIVE_AMS: The positive aggregator membership of the possible states in
 * the disseminator.
 *
 * 3. POSITIVE_AMS_FP: The false positive probabilities of (2).
 *
 * 4. POSITIVE_AMS: Indicates a positive aggregator membership in a disseminator
 *
 * 5. AMD_FP: The false positive probability of (4).
 *
 * @author Evangelos
 */
public enum DisseminatorReport {
    SELECTED_STATE,
    POSITIVE_AMS,
    POSITIVE_AMS_FP,
    POSITIVE_AMD,
    AMD_FP,
}
