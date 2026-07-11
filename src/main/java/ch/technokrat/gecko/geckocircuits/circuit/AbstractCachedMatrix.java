package ch.technokrat.gecko.geckocircuits.circuit;

/**
 * Base class for cached LU-decomposed matrices used in circuit simulation.
 * Caching is keyed by matrix content hash codes to avoid redundant
 * factorizations of identical system matrices.
 */
abstract class AbstractCachedMatrix {

    protected int _hashCode = -1;
    protected long _secondHashCode = -1;

    // use prime numbers, here:
    protected static final int HASH_7 = 7;
    protected static final int HASH_13 = 13;
    protected static final int HASH_17 = 17;
    protected static final int HASH_23 = 23;
    protected static final int HASH_37 = 37;

    private static final int INT_LENGTH = 32;
    private double _latestAccessTime = -1;
    private int _accessCounter = 0;
    protected double[][] _originalMatrix;

    public AbstractCachedMatrix(final double[][] matrix) {
        _originalMatrix = matrix;
    }

    /**
     * Initializes the LU decomposition cache for the stored matrix.
     */
    abstract void initLUDecomp();

    /**
     * Removes the cached LU decomposition, freeing memory.
     */
    abstract public void deleteCache();

    /**
     * Solves the linear system A*x = b using the cached LU decomposition.
     *
     * @param bVector the right-hand side vector
     * @return the solution vector x
     */
    abstract public double[] solve(final double[] bVector);

    /**
     * Estimates the number of bytes required to store the LU decomposition cache.
     *
     * @return estimated memory requirement in bytes
     */
    abstract int calculateMemoryRequirement();

    public long secondHashCode() {
        if (_secondHashCode == -1) {
            long newHashCode = 0;

            for (int i = 0; i < _originalMatrix.length; i++) {
                newHashCode += java.util.Arrays.hashCode(_originalMatrix[i]) * (991 * (i + 3));
            }
            _secondHashCode = newHashCode;
        }

        return _secondHashCode;
    }

    @Override
    public final int hashCode() {
        if (_hashCode == -1) {
            long newHashCode = HASH_13;
            for (int i = 0; i < _originalMatrix.length; i++) {
                newHashCode += java.util.Arrays.hashCode(_originalMatrix[i]) * (829 * (i + 7));                
            }
            _hashCode = (int) (newHashCode ^ (newHashCode >>> INT_LENGTH));
        }
        return _hashCode;
    }

    /**
     * Compares this cached matrix with another for element-wise equality.
     *
     * @param obj the object to compare
     * @return true if the matrices have identical dimensions and element values
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractCachedMatrix other = (AbstractCachedMatrix) obj;

        if (other._originalMatrix.length != this._originalMatrix.length) {
            return false;
        }

        for (int i = 0; i < _originalMatrix.length; i++) {
            for (int j = 0; j < _originalMatrix[0].length; j++) {
                if (_originalMatrix[i][j] != other._originalMatrix[i][j]) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Records that the cache was accessed at the given simulation time.
     *
     * @param time the current simulation time
     */
    protected void setAccess(final double time) {
        _accessCounter++;
        _latestAccessTime = time;
    }

    /**
     * @return the number of times this cache entry has been accessed
     */
    protected int getAccessCounter() {
        return _accessCounter;
    }

    /**
     * @return the simulation time of the most recent access
     */
    protected double getLatestAccessTime() {
        return _latestAccessTime;
    }
}
