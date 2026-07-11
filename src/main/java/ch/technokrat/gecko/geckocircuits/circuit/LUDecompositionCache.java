/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit;

import java.util.HashMap;
import java.util.Map;

/**
 * Main-Function: getCachedLUDecomposition. For switched converters, one and the
 * same LU decomposition is typically re-computed very often. Therefore, this
 * class provides a cache, which speeds up the calculation for larger matrices.
 *
 * @author andy
 */
public class LUDecompositionCache {

    private static final int MAX_CACHE_SIZE = 1000;
    private static int varMaxCacheSize = MAX_CACHE_SIZE;
    private final Map<Integer, AbstractCachedMatrix> _cachedMatrices = new HashMap<Integer, AbstractCachedMatrix>();
    private int _cacheHitCounter = 0;
    private int _cacheMissCounter = 0;
    private static final boolean USE_CACHE = true;
    private static long memoryBytes = 0;

    private static final long maxJVMMemory = Runtime.getRuntime().maxMemory();

    /**
     *
     * @param matrix the matrix to search in the LU-Cache
     * @param time actual simulationtime, needed for the cache overflow removal
     * algorithm
     * @return the matrix in the cache, including the LU-Decomposition
     */
    public AbstractCachedMatrix getCachedLUDecomposition(final double[][] matrix, final double time) {

        final AbstractCachedMatrix newMatrix = new CachedMatrix(matrix);
        final AbstractCachedMatrix fromCache = _cachedMatrices.get(newMatrix.hashCode());
        if (fromCache == null) {
            _cacheMissCounter++;
            newMatrix.setAccess(time);
            testForCacheShrink(time);
            newMatrix.initLUDecomp();
            if (USE_CACHE) {
                _cachedMatrices.put(newMatrix.hashCode(), newMatrix);
                memoryBytes += newMatrix.calculateMemoryRequirement();
            }
            return newMatrix;

        } else {
            if (fromCache.secondHashCode() != newMatrix.secondHashCode()) {
                // this is in case something goes really wrong (by accident same hash
                // code of two actually different matrices
                newMatrix.setAccess(time);
                testForCacheShrink(time);
                newMatrix.initLUDecomp();
                if (USE_CACHE) {
                    _cachedMatrices.put(newMatrix.hashCode(), newMatrix);
                    memoryBytes += newMatrix.calculateMemoryRequirement();
                }
                return newMatrix;
            }

            fromCache.setAccess(time);
            _cacheHitCounter++;
            return fromCache;
        }
    }

    private void printDebugMessages(final double time) {
        _cacheHitCounter++;

        System.out.println("cache size: " + _cachedMatrices.size());
        System.out.println("cache hits: " + _cacheHitCounter + " " + _cacheMissCounter + " " + (100.0 * _cacheHitCounter / (_cacheHitCounter + _cacheMissCounter)) + "%");
        System.out.println("memory requirement in MB: " + memoryBytes / 1024 / 1024);
    }

    /**
     * If the cache exceeds the maximum size, removes the oldest matrix
     * and the two least-accessed matrices that are older than a threshold.
     * @param time current simulation time
     */
    private void testForCacheShrink(double time) {
        if (_cachedMatrices.size() > varMaxCacheSize) {
            Integer oldestKey = -1;
            double oldestTime = 1e99;

            for (Map.Entry<Integer, AbstractCachedMatrix> entry : _cachedMatrices.entrySet()) {
                AbstractCachedMatrix tmp = entry.getValue();
                if (tmp.getLatestAccessTime() < oldestTime) {
                    oldestKey = entry.getKey();
                    oldestTime = tmp.getLatestAccessTime();
                }
            }

            AbstractCachedMatrix removed = _cachedMatrices.remove(oldestKey);
            memoryBytes -= removed.calculateMemoryRequirement();

            if (removed != null) {
                removed.deleteCache();
            }

            double accessMinimumAge = (oldestTime + time) / 2;
            removeLeastAccessedMatrices(accessMinimumAge);
        }

        calculateNewVarMaxCacheSize();
    }

    /**
     * Removes up to three least-accessed matrices that are older than the specified age.
     * @param accessMinimumAge minimum age threshold for removal consideration
     */
    private void removeLeastAccessedMatrices(double accessMinimumAge) {
        Integer leastAccessKey = -1;
        Integer secondLeastAccessKey = -1;
        Integer thirdLeastAccessKey = -1;

        int leastAccessCounter = Integer.MAX_VALUE;
        int secondLeastAccessCounter = Integer.MAX_VALUE;
        int thirdLeastAccessCounter = Integer.MAX_VALUE;

        for (Map.Entry<Integer, AbstractCachedMatrix> entry : _cachedMatrices.entrySet()) {
            AbstractCachedMatrix tmp = entry.getValue();
            if (tmp.getLatestAccessTime() < accessMinimumAge) {

                int accessCounter = tmp.getAccessCounter();

                if (accessCounter < thirdLeastAccessCounter) {
                    thirdLeastAccessCounter = accessCounter;
                    thirdLeastAccessKey = entry.getKey();
                } else if (accessCounter < secondLeastAccessCounter) {
                    secondLeastAccessCounter = accessCounter;
                    secondLeastAccessKey = entry.getKey();
                } else if (accessCounter < leastAccessCounter) {
                    leastAccessCounter = accessCounter;
                    leastAccessKey = entry.getKey();
                }
            }
        }

        if (leastAccessKey != -1) {
            AbstractCachedMatrix remove = _cachedMatrices.remove(leastAccessKey);
            memoryBytes -= remove.calculateMemoryRequirement();
        }

        if (secondLeastAccessKey != -1) {
            AbstractCachedMatrix remove = _cachedMatrices.remove(secondLeastAccessKey);
            memoryBytes -= remove.calculateMemoryRequirement();
        }

        if (thirdLeastAccessKey != -1) {
            AbstractCachedMatrix remove = _cachedMatrices.remove(thirdLeastAccessKey);
            memoryBytes -= remove.calculateMemoryRequirement();
        }
    }

    /**
     * Adjusts the maximum cache size based on current memory usage.
     * Reduces the limit if memory usage exceeds one third of max JVM memory.
     */
    private void calculateNewVarMaxCacheSize() {
        if (memoryBytes > maxJVMMemory / 3) {
            // enshure that the cache size is not too big.
            varMaxCacheSize = _cachedMatrices.size();
        }

        if (memoryBytes < maxJVMMemory / 10) {
            varMaxCacheSize = MAX_CACHE_SIZE;
        }
    }
}
