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
 * Four types of bloom filters are defined in DIAS:
 * 
 * 1. Aggregator Membership of a State (AMS)
 *      Simple or counting
 * 
 * 2. Aggregator Membership in a Disseminator (AMD)
 *      Simple
 * 
 * 3. Disseminator Membership in an Aggregator (DMA)
 *      Simple
 * 
 * 4. State membership in an Aggregate (SMA)
 *      Counting
 * 
 * All the bloom filters of DIAS are parameterized the 3 paramerers:
 * 
 *  a. The hash type: Simple, Double, Triple
 *  b. m: the size of the vector
 *  c. k: the number of hash functions
 * 
 * The parameters influence the probability of false postives that appear on
 * them. The number of items that are about to be added must be estimated for an
 * effective choice of these parameters.
 *
 *
 * @author Evangelos
 */
public enum BloomFilterParams {
    AMS_TYPE,
    AMS_HASH_TYPE,
    AMS_M,
    AMS_K,

    DMA_HASH_TYPE,
    DMA_M,
    DMA_K,

    AMD_HASH_TYPE,
    AMD_M,
    AMD_K,

    SMA_HASH_TYPE,
    SMA_M,
    SMA_K,
}
