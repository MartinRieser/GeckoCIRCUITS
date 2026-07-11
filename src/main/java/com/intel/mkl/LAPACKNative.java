package com.intel.mkl;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/**
 * Package-private class holding JNI native method declarations for LAPACK operations.
 * Delegates to the Intel MKL native library via JNI.
 *
 * @see LAPACK
 */
 class LAPACKNative {

    /**
     * Loads platform-specific native libraries. On Windows, libiomp5ui is required for OpenMP support.
     */
    static {                
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            System.loadLibrary( "libiomp5ui" );
        }
        System.loadLibrary("mkl_java_stubs");
    }

    // ----- Native methods added by andy for GeckoCIRCUITS integration -----

    /**
     * PARDISO sparse direct solver (native implementation).
     * @param pt internal work array of size 64
     * @param maxfct maximum number of factors in memory
     * @param mnum current factorization number
     * @param mtype matrix type
     * @param phase control phase
     * @param n order of the matrix
     * @param values non-zero values in CSR format
     * @param ai row pointers for CSR format
     * @param aj column indices for CSR format
     * @param idum dummy parameter
     * @param nrhs number of right-hand sides
     * @param iparm parameter array
     * @param msglvl message level
     * @param ddum dummy array
     * @param ddum0 dummy array
     * @param error error flag
     * @return info result code
     */
    static native int PARADISO(int[] pt, int maxfct, int mnum, int mtype, int phase, int n, double[] values, int[] ai, int[] aj, int idum, int nrhs, int[] iparm, int msglvl, double[] ddum, double[] ddum0, int error);
        
    /** Solves a system of linear equations (complex float). */
    static native int cgesv(int n, int nrhs, float[] a, int lda, int[] ipiv, float[] b, int ldb);
    /** Solves a system of linear equations (float). */
    static native int sgesv(int n, int nrhs, float[] a, int lda, int[] ipiv, float[] b, int ldb);
    /** Solves a system of linear equations (double). */
    static native int dgesv(int n, int nrhs, double[] a,int lda, int[] ipiv, double[] b,int ldb);
    /** Solves a system of linear equations (complex double). */
    static native int zgesv(int n, int nrhs, double[] a,int lda, int[] ipiv, double[] b,int ldb);

    /** Computes eigenvalues for a symmetric matrix (float). */
    static native int ssyev(int jobz, int uplo, int n, float[] a,int lda, float[] w);
    /** Computes eigenvalues for a symmetric matrix (double). */
    static native int dsyev(int jobz, int uplo, int n, double[] a,int lda, double[] w);
    
    /** Computes eigenvalues for a general matrix (float). */
    static native int sgeev(int jobvl, int jobvr, int n, float[] a, int lda, float[] wr, float[] wi, float[] vl, int ldvl, float[] vr, int ldvr);
    /** Computes eigenvalues for a general matrix (double). */
    static native int dgeev(int jobvl, int jobvr, int n, double[] a, int lda, double[] wr, double[] wi, double[] vl, int ldvl, double[] vr, int ldvr);
    /** Computes eigenvalues for a general matrix (complex float). */
    static native int cgeev(int jobvl, int jobvr, int n, float[] a, int lda, float[] w, float[] vl, int ldvl, float[] vr, int ldvr);
    /** Computes eigenvalues for a general matrix (complex double). */
    static native int zgeev(int jobvl, int jobvr, int n, double[] a, int lda, double[] w, double[] vl, int ldvl, double[] vr, int ldvr);

    /** Computes the SVD of a general matrix (float). */
    static native int sgesvd(int jobu, int jobvt, int m, int n, float[] a, int lda, float[] s, float[] u, int ldu, float[] vt, int ldvt, float[] sd);
    /** Computes the SVD of a general matrix (double). */
    static native int dgesvd(int jobu, int jobvt, int m, int n, double[] a, int lda, double[] s, double[] u, int ldu, double[] vt, int ldvt, double[] sd);
    /** Computes the SVD of a general matrix (complex float). */
    static native int cgesvd(int jobu, int jobvt, int m, int n, float[] a, int lda, float[] s, float[] u, int ldu, float[] vt, int ldvt, float[] sd);
    /** Computes the SVD of a general matrix (complex double). */
    static native int zgesvd(int jobu, int jobvt, int m, int n, double[] a, int lda, double[] s, double[] u, int ldu, double[] vt, int ldvt, double[] sd);

    /** Solves a system using LU factorization (complex double). */
    static native int zgetrs(int trans, int n, int nrhs, double[] a, int lda , int[] ipiv, double[] b, int ldb);
    /** Solves a system using LU factorization with DoubleBuffer (complex double). */
    static native int zgetrs2(int trans, int n, int nrhs, DoubleBuffer a, int lda , int[] ipiv, double[] b, int ldb);
    /** Solves a system using LU factorization (complex float). */
    static native int cgetrs(int trans, int n, int nrhs, float[] a, int lda , int[] ipiv, float[] b, int ldb);
    /** Solves a system using LU factorization (float). */
    static native int sgetrs(int trans, int n, int nrhs, float[] a, int lda , int[] ipiv, float[] b, int ldb);
    /** Solves a system using LU factorization (double). */
    static native int dgetrs(int trans, int n, int nrhs, double[] a, int lda , int[] ipiv, double[] b, int ldb);
    /** LU factorization (complex double). */
    static native int zgetrf(int m, int n, double[] a, int lda, int[] ipiv);
    /** LU factorization with DoubleBuffer (complex double). */
    static native int zgetrf2(int m, int n, DoubleBuffer a, int lda, int[] ipiv);
    /** LU factorization (complex float). */
    static native int cgetrf(int m, int n, float[] a, int lda, int[] ipiv);
    /** LU factorization (double). */
    static native int dgetrf(int m, int n, double[] a, int lda, int[] ipiv);
    /** Matrix inversion using LU factorized matrix (double). */
    static native int dgetri(int n, double[] a, int lda, int[] ipiv, double[] work, int lwork);
    /** Matrix inversion using LU factorized matrix (complex double). */
    static native int zgetri(int n, double[] a, int lda, int[] ipiv, double[] work, int lwork);
    /** Matrix inversion using LU factorized matrix (complex float). */
    static native int cgetri(int n, float[] a, int lda, int[] ipiv, float[] work, int lwork);

    /** LU factorization (float). */
    static native int sgetrf(int m, int n, float[] a, int lda, int[] ipiv);
    /** Matrix inversion using LU factorized matrix (float). */
    static native int sgetri(int n, float[] a, int lda, int[] ipiv, float[] work, int lwork);

    /** Symmetric indefinite matrix inversion (complex double). */
    static native int zsytri(char uplo, int n, double[] a, int lda, int[] ipiv, double[] work);
    /** Symmetric indefinite matrix factorization (complex double). */
    static native int zsytrf(char uplo, int n, double[] a, int lda, int[] ipiv, double[] work, int lwork);
    /** Solves a system using symmetric indefinite factorization (complex double). */
    static native int zsytrs(char uplo, int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb);

    /** Cholesky factorization (float). */
    static native int spotrf(int uplo, int n, float[] a, int lda);
    /** Matrix inversion using Cholesky factorization (float). */
    static native int spotri(int uplo, int n, float[] a, int lda);

    /** Cholesky factorization in packed format (float). */
    static native int spptrf(int uplo, int n, float[] a, int lda);
    /** Matrix inversion using Cholesky factorization in packed format (float). */
    static native int spptri(int uplo, int n, float[] a, int lda);

    /** Cholesky factorization with FloatBuffer (float packed). */
    static native int spptrf2(int uplo, int n, FloatBuffer fb, int lda);
    /** Matrix inversion using Cholesky with FloatBuffer (float packed). */
    static native int spptri2(int uplo, int n, FloatBuffer fb, int lda);

    /** Matrix inversion using Cholesky factorization (double). */
    static native int dpotri(int uplo, int n, double[] a, int lda);
    /** Cholesky factorization (double). */
    static native int dpotrf(int uplo, int n, double[] a, int lda);

    /** Cholesky factorization in packed format (complex float). */
    static native int cpptrf(char uplo, int n, float[] a);
    /** Matrix inversion using Cholesky factorization in packed format (complex float). */
    static native int cpptri(char uplo, int n, float[] a);

    /** Symmetric packed matrix factorization (complex float). */
    static native int csptrf(char c, int n, float[] array, int[] ipiv);
    /** Symmetric packed matrix inversion (complex float). */
    static native int csptri(char c, int n, float[] array, float[] work, int[] ipiv);
    /** Solves a system using symmetric packed factorization (complex float). */
    static native int csptrs(char uplo, int n, int nrhs, float[] a, int[] ipiv, float[] b, int ldb);

    /** Symmetric packed matrix inversion (complex double). */
    static native int zsptri(char uplo, int n, double[] a, int[] ipiv, double[] work);
    /** Symmetric packed matrix factorization (complex double). */
    static native int zsptrf(char uplo, int n, double[] a, int[] ipiv);
    /** Solves a system using symmetric packed factorization (complex double). */
    static native int zsptrs(char uplo, int n, int nrhs, double[] a, int[] ipiv, double[] b, int ldb);

    /** Condition number estimation (float). */
    static native int sgecon(char norm, int n, float[] a, int lda, float anorm, float[] rcond);
    /** Condition number estimation (double). */
    static native int dgecon(char norm, int n, double[] a, int lda, double anorm, double[] rcond);
    /** Condition number estimation (complex double). */
    static native int zgecon(char norm, int n, double[] a, int lda, double anorm, double[] rcond);

    /** Row and column equilibration (complex double). */
    static native int zgeequ(int m, int n, double[] a, int lda, double[] r, double[] c, double[] rowcnd, double[] colcnd, double[] amax);
    /** Row and column equilibration with DoubleBuffer (complex double). */
    static native int zgeequ2(int m, int n, DoubleBuffer a, int lda, double[] r, double[] c, double[] rowcnd, double[] colcnd, double[] amax);

    /** Scales a matrix using row/column factors (complex double). */
    static native int zlaqge(int m, int n, double[] a, int lda, double[] r, double[] c, double[] rowcnd, double[] colcnd, double[] amax, char[] equed);

    /** Refines the solution of a packed system (complex double). */
    static native int zsprfs(char uplo, int n, int nrhs, double[] af, double[] afp, int[] ipiv, double[] b, int ldb, double[] x, int ldx, double[] ferr, double[] berr, double[] work, double[] rwork);
    /** Refines the solution of a packed system (complex float). */
    static native int csprfs(char uplo, int n, int nrhs, float[] af, float[] afp, int[] ipiv, float[] b, int ldb, float[] x, int ldx, float[] ferr, float[] berr, float[] work, float[] rwork);

    /** Scales a matrix using row/column factors (complex float). */
    static native int claqge(int m, int n, float[] a, int lda, float[] r, float[] c, float[] rowcnd, float[] colcnd, float[] amax, char[] equed);
    /** Row and column equilibration (complex float). */
    static native int cgeequ(int m, int n, float[] a, int lda, float[] r, float[] c, float[] rowcnd, float[] colcnd, float[] amax);
 }
