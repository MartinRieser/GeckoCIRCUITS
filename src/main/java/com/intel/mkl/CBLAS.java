package com.intel.mkl;


/**
 * Wrappers to CBLAS functions from Intel MKL. The CBLAS
 * interface is used for accessing to BLAS functions.
 * See MKL Reference Manual, Chapter 2 "BLAS and Sparse BLAS
 * Routines", and appendix D "CBLAS Interface to the BLAS".
 *
 * <p>This is demo wrapper which processes only few CBLAS
 * functions in order to demonstrate:
 * <ul>
 * <li>binding MKL/CBLAS functions with Java
 * <li>encoding of 1- and 2-dimensional arrays
 * <li>encoding of complex numbers
 * </ul>
 *
 * <p>CAUTION: This demo wrapper does not:
 * <ul>
 *   <li>demonstrate using huge arrays (>2 billion elements)
 *   <li>demonstrate processing arrays in native memory
 *   <li>check correctness of function parameters
 *   <li>demonstrate performance optimizations
 * </ul>
 *
 * <p>These wrappers assume using 1-dimensional Java arrays of
 * the type double[] or float[] to process floating-point data.
 * A 2-dimensional data series is stored to 1-dimensional array
 * as column-major or row-major 1-dimensional data series.
 *
 * <p>A complex number is stored as 2-elements array: real part
 * of the number as the 1st element of the array, and imaginary
 * part as the 2nd.
 *
 * <p>A complex data series of the length N is stored as a real
 * series of the length 2*N. The n-th complex element is stored
 * as the pair of 2*n-th and (2*n+1)-th real elements: the even
 * indexed for real part and the odd indexed for imaginary part.
 *
 * <p>For more details, please see the MKL User's Guide.
 *
 * <p>All methods in this class are {@code native} and delegate directly to the
 * Intel MKL shared library via JNI. No Java-side implementation is provided.</p>
 */
public final class CBLAS {

       
    //////////////////////////////////////////////////////////

    /**
     * Instantiation is disabled.
     */
    private CBLAS() {}

    /**
     * Load the stubs to the native MKL functions.
     */
    static {
        if(System.getProperty("os.name").toLowerCase().contains("windows")) {
            System.loadLibrary( "libiomp5ui" ); 
            System.loadLibrary("libiomp5md");
        } else {
            System.loadLibrary("iomp5");
        }
        
        System.loadLibrary("mkl_java_stubs");

    }

    //////////////////////////////////////////////////////////

    /** Constants for CBLAS_ORDER enum. */
    public final static class ORDER {
        private ORDER() {}
        /** row-major arrays */
        public final static int RowMajor=101;
        /** column-major arrays */
        public final static int ColMajor=102;
    }

    /** Constants for CBLAS_TRANSPOSE enum. */
    public final static class TRANSPOSE {
        private TRANSPOSE() {}
        /** trans='N' */
        public final static int NoTrans  =111;
        /** trans='T' */
        public final static int Trans    =112;
        /** trans='C' */
        public final static int ConjTrans=113;
    }

    /** Constants for CBLAS_UPLO enum. */
    public final static class UPLO {
        private UPLO() {}
        /** uplo ='U' */
        public final static int Upper=121;
        /** uplo ='L' */
        public final static int Lower=122;
    }

    /** Constants for CBLAS_DIAG enum. */
    public final static class DIAG {
        private DIAG() {}
        /** diag ='N' */
        public final static int NonUnit=131;
        /** diag ='U' */
        public final static int Unit   =132;
    }

    /** Constants for CBLAS_SIDE enum. */
    public final static class SIDE {
        private SIDE() {}
        /** side ='L' */
        public final static int Left =141;
        /** side ='R' */
        public final static int Right=142;
    }

    //////////////////////////////////////////////////////////

    /**
     * Computes a matrix-matrix product with general matrices: C := alpha*op(A)*op(B) + beta*C (single precision).
     * @param Order Specifies row-major (101) or column-major (102) ordering.
     * @param TransA Specifies the transpose operation for A: NoTrans(111), Trans(112), or ConjTrans(113).
     * @param TransB Specifies the transpose operation for B: NoTrans(111), Trans(112), or ConjTrans(113).
     * @param M Number of rows of the matrix op(A) and of the matrix C.
     * @param N Number of columns of the matrix op(B) and of the matrix C.
     * @param K Number of columns of the matrix op(A) and number of rows of the matrix op(B).
     * @param alpha Scaling scalar alpha.
     * @param A Array holding matrix A.
     * @param lda The leading dimension of A as declared in the calling program.
     * @param B Array holding matrix B.
     * @param ldb The leading dimension of B as declared in the calling program.
     * @param beta Scaling scalar beta.
     * @param C Array holding matrix C.
     * @param ldc The leading dimension of C as declared in the calling program.
     */
    public static native void sgemm(int Order, int TransA, int TransB, int M, int N, int K,
        float alpha, float[] A, int lda, float[] B, int ldb, float beta, float[] C, int ldc);

    /**
     * Computes a matrix-matrix product with general matrices: C := alpha*op(A)*op(B) + beta*C (double precision).
     * @param Order Specifies row-major (101) or column-major (102) ordering.
     * @param TransA Specifies the transpose operation for A: NoTrans(111), Trans(112), or ConjTrans(113).
     * @param TransB Specifies the transpose operation for B: NoTrans(111), Trans(112), or ConjTrans(113).
     * @param M Number of rows of the matrix op(A) and of the matrix C.
     * @param N Number of columns of the matrix op(B) and of the matrix C.
     * @param K Number of columns of the matrix op(A) and number of rows of the matrix op(B).
     * @param alpha Scaling scalar alpha.
     * @param A Array holding matrix A.
     * @param lda The leading dimension of A as declared in the calling program.
     * @param B Array holding matrix B.
     * @param ldb The leading dimension of B as declared in the calling program.
     * @param beta Scaling scalar beta.
     * @param C Array holding matrix C.
     * @param ldc The leading dimension of C as declared in the calling program.
     */
    public static native void dgemm(int Order, int TransA, int TransB, int M, int N, int K,
        double alpha, double[] A, int lda, double[] B, int ldb, double beta, double[] C, int ldc);

    /**
     * Computes a matrix-matrix product with general matrices: C := alpha*op(A)*op(B) + beta*C (single precision complex).
     * @param Order Specifies row-major (101) or column-major (102) ordering.
     * @param TransA Specifies the transpose operation for A: NoTrans(111), Trans(112), or ConjTrans(113).
     * @param TransB Specifies the transpose operation for B: NoTrans(111), Trans(112), or ConjTrans(113).
     * @param M Number of rows of the matrix op(A) and of the matrix C.
     * @param N Number of columns of the matrix op(B) and of the matrix C.
     * @param K Number of columns of the matrix op(A) and number of rows of the matrix op(B).
     * @param alpha Two-element array specifying the complex scalar alpha (real, imaginary).
     * @param A Array holding matrix A.
     * @param lda The leading dimension of A as declared in the calling program.
     * @param B Array holding matrix B.
     * @param ldb The leading dimension of B as declared in the calling program.
     * @param beta Two-element array specifying the complex scalar beta (real, imaginary).
     * @param C Array holding matrix C.
     * @param ldc The leading dimension of C as declared in the calling program.
     */
    public static native void cgemm(int Order, int TransA, int TransB, int M, int N, int K,
        float[] alpha, float[] A, int lda, float[] B, int ldb, float[] beta, float[] C, int ldc);

    /**
     * Computes a matrix-matrix product with general matrices: C := alpha*op(A)*op(B) + beta*C (double precision complex).
     * @param Order Specifies row-major (101) or column-major (102) ordering.
     * @param TransA Specifies the transpose operation for A: NoTrans(111), Trans(112), or ConjTrans(113).
     * @param TransB Specifies the transpose operation for B: NoTrans(111), Trans(112), or ConjTrans(113).
     * @param M Number of rows of the matrix op(A) and of the matrix C.
     * @param N Number of columns of the matrix op(B) and of the matrix C.
     * @param K Number of columns of the matrix op(A) and number of rows of the matrix op(B).
     * @param alpha Two-element array specifying the complex scalar alpha (real, imaginary).
     * @param A Array holding matrix A.
     * @param lda The leading dimension of A as declared in the calling program.
     * @param B Array holding matrix B.
     * @param ldb The leading dimension of B as declared in the calling program.
     * @param beta Two-element array specifying the complex scalar beta (real, imaginary).
     * @param C Array holding matrix C.
     * @param ldc The leading dimension of C as declared in the calling program.
     */
    public static native void zgemm(int Order, int TransA, int TransB, int M, int N, int K,
        double[] alpha, double[] A, int lda, double[] B, int ldb, double[] beta, double[] C, int ldc);
    
    
    /**
     * Computes a matrix-vector product using a general matrix y := alpha*A*x + beta*y;.
     * @param order the order of the matrix
     * @param TransA Specifies the operation: if trans= 'N' or 'n', then
     * @param M Specifies the number of rows of the matrix A. The value of m must be at least zero.
     * @param N Specifies the number of columns of the matrix A. The value of n must be at least zero.
     * @param alpha Specifies the scalar alpha.
     * @param A Before entry, the leading m-by-n part of the array a must contain the matrix of coefficients.
     * @param lda Specifies the first dimension of a as declared in the calling (sub)program. The value of lda must be at least max(1, m).
     * @param X Array, DIMENSION at least (1+(n-1)*abs(incx)) when trans = 'N' or 'n' and at least (1+(m - 1)*abs(incx)) otherwise. Before entry, the incremented array x must contain the vector x.
     * @param incX Specifies the increment for the elements of x. The value of incx must not be zero.
     * @param beta Specifies the scalar beta. When beta is set to zero, then y need not be set on input.
     * @param Y Array, DIMENSION at least (1 +(m - 1)*abs(incy)) when trans = 'N' or 'n' and at least (1 +(n - 1)*abs(incy)) otherwise. Before entry with non-zero beta, the incremented array y must contain the vector y.
     * @param incY Specifies the increment for the elements of y. The value of incy must not be zero.
     */
    public static native void sgemv(int order, int TransA, int M, int N,
        float alpha, float[] A, int lda, float[] X, int incX, float beta, float[] Y, int incY);

    /**
     * Computes a matrix-vector product using a general matrix y := alpha*A*x + beta*y;.
     * @param order the order of the matrix
     * @param TransA Specifies the operation: if trans= 'N' or 'n', then
     * @param M Specifies the number of rows of the matrix A. The value of m must be at least zero.
     * @param N Specifies the number of columns of the matrix A. The value of n must be at least zero.
     * @param alpha Specifies the scalar alpha.
     * @param A Before entry, the leading m-by-n part of the array a must contain the matrix of coefficients.
     * @param lda Specifies the first dimension of a as declared in the calling (sub)program. The value of lda must be at least max(1, m).
     * @param X Array, DIMENSION at least (1+(n-1)*abs(incx)) when trans = 'N' or 'n' and at least (1+(m - 1)*abs(incx)) otherwise. Before entry, the incremented array x must contain the vector x.
     * @param incX Specifies the increment for the elements of x. The value of incx must not be zero.
     * @param beta Specifies the scalar beta. When beta is set to zero, then y need not be set on input.
     * @param Y Array, DIMENSION at least (1 +(m - 1)*abs(incy)) when trans = 'N' or 'n' and at least (1 +(n - 1)*abs(incy)) otherwise. Before entry with non-zero beta, the incremented array y must contain the vector y.
     * @param incY Specifies the increment for the elements of y. The value of incy must not be zero.
     */
    public static native void dgemv(int order, int TransA,int M, int N,
        double alpha, double[] A, int lda, double[] X, int incX, double beta, double[] Y, int incY);

    /**
     * Computes a matrix-vector product using a general matrix y := alpha*A*x + beta*y;.
     * @param order the order of the matrix
     * @param TransA Specifies the operation: if trans= 'N' or 'n', then
     * @param M Specifies the number of rows of the matrix A. The value of m must be at least zero.
     * @param N Specifies the number of columns of the matrix A. The value of n must be at least zero.
     * @param alpha Specifies the scalar alpha.
     * @param A Before entry, the leading m-by-n part of the array a must contain the matrix of coefficients.
     * @param lda Specifies the first dimension of a as declared in the calling (sub)program. The value of lda must be at least max(1, m).
     * @param X Array, DIMENSION at least (1+(n-1)*abs(incx)) when trans = 'N' or 'n' and at least (1+(m - 1)*abs(incx)) otherwise. Before entry, the incremented array x must contain the vector x.
     * @param incX Specifies the increment for the elements of x. The value of incx must not be zero.
     * @param beta Specifies the scalar beta. When beta is set to zero, then y need not be set on input.
     * @param Y Array, DIMENSION at least (1 +(m - 1)*abs(incy)) when trans = 'N' or 'n' and at least (1 +(n - 1)*abs(incy)) otherwise. Before entry with non-zero beta, the incremented array y must contain the vector y.
     * @param incY Specifies the increment for the elements of y. The value of incy must not be zero.
     */
    public static native void cgemv(int order, int TransA, int M, int N,
        float[] alpha, float[] A, int lda, float[] X, int incX, float[] beta, float[] Y, int incY);

    /**
     * Computes a matrix-vector product using a general matrix y := alpha*A*x + beta*y;.
     * @param order the order of the matrix
     * @param TransA Specifies the operation: if trans= 'N' or 'n', then
     * @param M Specifies the number of rows of the matrix A. The value of m must be at least zero.
     * @param N Specifies the number of columns of the matrix A. The value of n must be at least zero.
     * @param alpha Specifies the scalar alpha.
     * @param A Before entry, the leading m-by-n part of the array a must contain the matrix of coefficients.
     * @param lda Specifies the first dimension of a as declared in the calling (sub)program. The value of lda must be at least max(1, m).
     * @param X Array, DIMENSION at least (1+(n-1)*abs(incx)) when trans = 'N' or 'n' and at least (1+(m - 1)*abs(incx)) otherwise. Before entry, the incremented array x must contain the vector x.
     * @param incX Specifies the increment for the elements of x. The value of incx must not be zero.
     * @param beta Specifies the scalar beta. When beta is set to zero, then y need not be set on input.
     * @param Y Array, DIMENSION at least (1 +(m - 1)*abs(incy)) when trans = 'N' or 'n' and at least (1 +(n - 1)*abs(incy)) otherwise. Before entry with non-zero beta, the incremented array y must contain the vector y.
     * @param incY Specifies the increment for the elements of y. The value of incy must not be zero.
     */
    public static native void zgemv(int order, int TransA, int M, int N,
        double[] alpha, double[] A, int lda, double[] X, int incX, double[] beta, double[] Y, int incY);

    //////////////////////////////////////////////////////////

    /**
     * Computes the dot product of two single-precision vectors.
     * @param N Number of elements in the vectors.
     * @param X Array holding vector x.
     * @param incX Stride between consecutive elements of x.
     * @param Y Array holding vector y.
     * @param incY Stride between consecutive elements of y.
     * @return The dot product of x and y.
     */
    public static native float sdot(int N, float[] X, int incX, float[] Y, int incY);

    /**
     * Computes the dot product of two double-precision vectors.
     * @param N Number of elements in the vectors.
     * @param X Array holding vector x.
     * @param incX Stride between consecutive elements of x.
     * @param Y Array holding vector y.
     * @param incY Stride between consecutive elements of y.
     * @return The dot product of x and y.
     */
    public static native double ddot(int N, double[] X, int incX, double[] Y, int incY);

    /**
     * Computes the conjugate dot product of two single-precision complex vectors.
     * @param N Number of elements in the vectors.
     * @param X Array holding vector x (interleaved real/imaginary pairs).
     * @param incX Stride between consecutive elements of x.
     * @param Y Array holding vector y (interleaved real/imaginary pairs).
     * @param incY Stride between consecutive elements of y.
     * @param dotc Output array (2 elements: real, imaginary) receiving the result.
     */
    public static native void cdotc_sub(int N, float[] X, int incX, float[] Y, int incY, float[] dotc);

    /**
     * Computes the conjugate dot product of two double-precision complex vectors.
     * @param N Number of elements in the vectors.
     * @param X Array holding vector x (interleaved real/imaginary pairs).
     * @param incX Stride between consecutive elements of x.
     * @param Y Array holding vector y (interleaved real/imaginary pairs).
     * @param incY Stride between consecutive elements of y.
     * @param dotc Output array (2 elements: real, imaginary) receiving the result.
     */
    public static native void zdotc_sub(int N, double[] X, int incX, double[] Y, int incY, double[] dotc);

    /**
     * Computes the unconjugated dot product of two single-precision complex vectors.
     * @param N Number of elements in the vectors.
     * @param X Array holding vector x (interleaved real/imaginary pairs).
     * @param incX Stride between consecutive elements of x.
     * @param Y Array holding vector y (interleaved real/imaginary pairs).
     * @param incY Stride between consecutive elements of y.
     * @param dotu Output array (2 elements: real, imaginary) receiving the result.
     */
    public static native void cdotu_sub(int N, float[] X, int incX, float[] Y, int incY, float[] dotu);

    /**
     * Computes the unconjugated dot product of two double-precision complex vectors.
     * @param N Number of elements in the vectors.
     * @param X Array holding vector x (interleaved real/imaginary pairs).
     * @param incX Stride between consecutive elements of x.
     * @param Y Array holding vector y (interleaved real/imaginary pairs).
     * @param incY Stride between consecutive elements of y.
     * @param dotu Output array (2 elements: real, imaginary) receiving the result.
     */
    public static native void zdotu_sub(int N, double[] X, int incX, double[] Y, int incY, double[] dotu);

    /**
     * Converts a dense matrix to sparse CSR format (double precision).
     * @param job Integer array (6 elements) specifying conversion direction and parameters.
     * @param m Number of rows in the dense matrix.
     * @param n Number of columns in the dense matrix.
     * @param adns Dense matrix values in column-major or row-major order.
     * @param lda Leading dimension of the dense matrix.
     * @param acsr Output array containing the CSR-format values.
     * @param aj Output array containing the column indices for CSR format.
     * @param ai Output array containing the row start indices for CSR format.
     * @return Status info: 0 on success, 1 if insufficient space, etc.
     */
    //public static native int ddnscsr(int[] job, int m, int n, double[] adns, int lda, double[] acsr, int[] aj, int[] ai);
    public static native int ddnscsr(int[] job, int m, int n, double[] adns, int lda, double[] acsr, int[] aj, int[] ai);


    /**
     * Computes a sparse matrix-matrix product in CSR format (double precision): C := alpha*A*B + beta*C.
     * @param transa Operation type for A: 'N' (no transpose), 'T' (transpose), or 'C' (conjugate transpose).
     * @param m Number of rows of matrix A.
     * @param n Number of columns of matrix C.
     * @param k Number of columns of matrix A / rows of matrix B.
     * @param alpha Scaling scalar alpha.
     * @param matdescra Character array specifying matrix attributes (general, symmetric, etc.).
     * @param val Array of non-zero values of the sparse matrix A.
     * @param index Column indices of non-zero elements in A.
     * @param pntrb Array of row start indices for CSR format.
     * @param pntre Array of row end indices for CSR format.
     * @param b Dense matrix B.
     * @param ldb Leading dimension of B.
     * @param beta Scaling scalar beta.
     * @param c Output dense matrix C.
     * @param ldc Leading dimension of C.
     */
    public static native void dcsrmm(char transa, int m, int n, int k, double[] alpha, char[] matdescra, double[] val,
            int[] index, int[] pntrb, int[] pntre, double[] b, int ldb, double[] beta, double[] c , int ldc);

    /**
     * Computes a sparse matrix-matrix product in CSR format (single precision): C := alpha*A*B + beta*C.
     * @param transa Operation type for A: 'N' (no transpose), 'T' (transpose), or 'C' (conjugate transpose).
     * @param m Number of rows of matrix A.
     * @param n Number of columns of matrix C.
     * @param k Number of columns of matrix A / rows of matrix B.
     * @param alpha Scaling scalar alpha.
     * @param matdescra Character array specifying matrix attributes.
     * @param val Array of non-zero values of the sparse matrix A.
     * @param index Column indices of non-zero elements in A.
     * @param pntrb Array of row start indices for CSR format.
     * @param pntre Array of row end indices for CSR format.
     * @param b Dense matrix B.
     * @param ldb Leading dimension of B.
     * @param beta Scaling scalar beta.
     * @param c Output dense matrix C.
     * @param ldc Leading dimension of C.
     */
    public static native void scsrmm(char transa, int m, int n, int k, float[] alpha, char[] matdescra, float[] val,
            int[] index, int[] pntrb, int[] pntre, float[] b, int ldb, float[] beta, float[] c , int ldc);


    /**
     * Computes a sparse matrix-matrix product in CSR format (double-precision complex): C := alpha*A*B + beta*C.
     * @param transa Operation type for A: 'N' (no transpose), 'T' (transpose), or 'C' (conjugate transpose).
     * @param m Number of rows of matrix A.
     * @param n Number of columns of matrix C.
     * @param k Number of columns of matrix A / rows of matrix B.
     * @param alpha Two-element array specifying the complex scalar alpha (real, imaginary).
     * @param matdescra Character array specifying matrix attributes.
     * @param val Array of non-zero values of the sparse matrix A (interleaved real/imaginary pairs).
     * @param index Column indices of non-zero elements in A.
     * @param pntrb Array of row start indices for CSR format.
     * @param pntre Array of row end indices for CSR format.
     * @param b Dense matrix B (interleaved real/imaginary pairs).
     * @param ldb Leading dimension of B.
     * @param beta Two-element array specifying the complex scalar beta (real, imaginary).
     * @param c Output dense matrix C (interleaved real/imaginary pairs).
     * @param ldc Leading dimension of C.
     */
    public static native void zcsrmm(char transa, int m, int n, int k, double[] alpha, char[] matdescra, double[] val,
            int[] index, int[] pntrb, int[] pntre, double[] b, int ldb, double[] beta, double[] c , int ldc);

    /**
     * Computes a matrix-vector product using a symmetric packed matrix: y := alpha*A*x + beta*y
     * @param uplo Specifies whether the upper or lower triangular part of the
     * matrix A is supplied in the packed array ap. uplo If uplo = 'U' or 'u',
     * then the upper triangular part of the matrix A is supplied in the packed
     * array ap . If uplo = 'L' or 'l', then the low triangular part of the
     * matrix A is supplied in the packed array ap .
     * @param n Specifies the order of the matrix a. The value of n must be at least zero.
     * @param alpha Specifies the scalar alpha.
     * @param ap Before entry with uplo = 'U' or 'u', the array ap must contain the
     * upper triangular part of the symmetric matrix packed sequentially, column-by-column,
     * so that ap(1) contains a(1,1), ap(2) and ap(3) contain a(1,2) and a(2, 2) respectively,
     * and so on. Before entry with uplo = 'L' or 'l', the array ap must contain the lower triangular
     * part of the symmetric matrix packed sequentially, column-by-column, so that ap(1) contains
     * a(1,1), ap(2) and ap(3) contain a(2,1) and a(3,1) respectively, and so on.
     * @param x Array, DIMENSION at least (1 + (n - 1)*abs(incx)). Before entry, the incremented
     * array x must contain the n-element vector x.
     * @param incx Specifies the increment for the elements of x. The value of incx must not be zero.
     * @param beta Specifies the scalar beta. When beta is supplied as zero, then y need not be set on input.
     * @param y Array, DIMENSION at least (1 + (n - 1)*abs(incy)). Before entry, the incremented array y
     * must contain the n-element vector y.
     * @param incy Specifies the increment for the elements of y. The value of incy must not be zero.
     */
    public static native void dspmv(int order, int uplo, int n, double alpha, double[] ap, double[] x, int incx, double beta, double[] y, int incy);

    /**
     * Computes a matrix-vector product using a symmetric packed matrix: y := A*x
     * @param n Specifies the order of the matrix. The value of n must be at least zero.
     * @param ap Before entry, the array ap must contain the symmetric matrix packed sequentially.
     * @param x Array of size at least n. Before entry, the array x must contain the vector x.
     * @param y Array of size at least n. On exit, the array y contains the resulting vector y.
     */
    public static native void cspmv(int n, float[] ap, float[] x, float[] y);


    /**
     * Computes a matrix-vector product using a symmetric packed matrix: y := alpha*A*x + beta*y
     * @param uplo Specifies whether the upper or lower triangular part of the
     * matrix A is supplied in the packed array ap. uplo If uplo = 'U' or 'u',
     * then the upper triangular part of the matrix A is supplied in the packed
     * array ap . If uplo = 'L' or 'l', then the low triangular part of the
     * matrix A is supplied in the packed array ap .
     * @param n Specifies the order of the matrix a. The value of n must be at least zero.
     * @param alpha Specifies the scalar alpha.
     * @param ap Before entry with uplo = 'U' or 'u', the array ap must contain the
     * upper triangular part of the symmetric matrix packed sequentially, column-by-column,
     * so that ap(1) contains a(1,1), ap(2) and ap(3) contain a(1,2) and a(2, 2) respectively,
     * and so on. Before entry with uplo = 'L' or 'l', the array ap must contain the lower triangular
     * part of the symmetric matrix packed sequentially, column-by-column, so that ap(1) contains
     * a(1,1), ap(2) and ap(3) contain a(2,1) and a(3,1) respectively, and so on.
     * @param x Array, DIMENSION at least (1 + (n - 1)*abs(incx)). Before entry, the incremented
     * array x must contain the n-element vector x.
     * @param incx Specifies the increment for the elements of x. The value of incx must not be zero.
     * @param beta Specifies the scalar beta. When beta is supplied as zero, then y need not be set on input.
     * @param y Array, DIMENSION at least (1 + (n - 1)*abs(incy)). Before entry, the incremented array y
     * must contain the n-element vector y.
     * @param incy Specifies the increment for the elements of y. The value of incy must not be zero.
     */
    public static native void sspmv(int order, int uplo, int n, float alpha, float[] ap, float[] x, int incx, float beta, float[] y, int incy);


    /**
     * Computes matrix - matrix product of a sparse matrix stored in the CSR format (single precision complex).
     * @param transa Operation type for A: 'N' (no transpose), 'T' (transpose), or 'C' (conjugate transpose).
     * @param m Number of rows of matrix A.
     * @param n Number of columns of matrix C.
     * @param k Number of columns of matrix A / rows of matrix B.
     * @param alpha Two-element array specifying the complex scalar alpha (real, imaginary).
     * @param matdescra Byte array specifying matrix attributes (general, symmetric, etc.).
     * @param A Array of non-zero values of the sparse matrix A (interleaved real/imaginary pairs).
     * @param columns Column indices of non-zero elements in A.
     * @param rowIndices Array of row start indices for CSR format.
     * @param rowIndices1 Array of row end indices for CSR format.
     * @param B Dense matrix B (interleaved real/imaginary pairs).
     * @param ldb Leading dimension of B.
     * @param beta Two-element array specifying the complex scalar beta (real, imaginary).
     * @param C Output dense matrix C (interleaved real/imaginary pairs).
     * @param ldc Leading dimension of C.
     */
      public static native void ccsrmm( byte transa, int m, int n, int k, float[] alpha, byte[] matdescra, 
                float[] A, int[] columns, int[] rowIndices, int[] rowIndices1, float[] B, int ldb, float[] beta, float[] C, int ldc);

    /**
     * The simatcopy routine performs scaling and in-place transposition/copying of matrices.
     * A transposition operation can be a normal matrix copy, a transposition, a conjugate transposition,
     * or just a conjugation. The operation is defined as follows: A := alpha*op(A).
     * @param ordering Ordering of the matrix storage.
     * @param Trans Parameter that specifies the operation type. If trans = 'N' or 'n', op(A)=A and the matrix A is
     * assumed unchanged on input. If trans = 'T' or 't', it is assumed that A should be transposed.
     * If trans = 'C' or 'c', it is assumed that A should be conjugate transposed. If trans = 'R' or 'r', it is 
     * assumed that A should be only conjugated. If the data is real, then trans = 'R' is the same as trans
     * = 'N', and trans = 'C' is the same as trans = 'T'.
     * @param rows The number of matrix rows.
     * @param cols The number of matrix columns.
     * @param array Array, DIMENSION a(scr_lda,*).
     * @param alpha This parameter scales the input matrix by alpha.
     * @param src_lda Distance between the first elements in adjacent columns (in the case of the column-major order) or rows
     * (in the case of the row-major order) in the source matrix; measured in the number of elements. src_lda This 
     * parameter must be at least max(1,rows) if ordering = 'C' or 'c', and max(1,cols) otherwise.
     * @param dst_lda Distance between the first elements in adjacent columns (in the case of the column-major order) or rows
     * (in the case of the row-major order) in the destination matrix; measured in the number of elements. dst_lda To determine 
     * the minimum value of dst_lda on output, consider the following guideline:If ordering = 'C' or 'c', then • If trans = 'T' or 't' or 'C' or 'c', this parameter
     * must be at least max(1,rows) • If trans = 'N' or 'n' or 'R' or 'r', this parameter must be at least max(1,cols) If ordering = 'R' or 'r', then
     * • If trans = 'T' or 't' or 'C' or 'c', this parameter must be at least max(1,cols) • If trans = 'N' or 'n' or 'R' or 'r', this parameter must be at least max(1,rows)
     */
    public static native void simatcopy(char ordering, char Trans, int rows, int cols, float[] array, float alpha, int src_lda, int dst_lda);

    /**
     * Computes the Euclidean (L2) norm of a double-precision vector.
     * @param ivar Number of elements in the vector.
     * @param expected_solution Array holding the vector.
     * @param i Stride between consecutive elements.
     * @return The 2-norm of the vector.
     */
    public static native double dnrm2(int ivar, double[] expected_solution, int i);

    /**
     * Initializes the FGMRES (Flexible GMRES) iterative solver for linear systems.
     * @param ivar Number of variables (size of the system).
     * @param computed_solution Array for the solution vector (initialized on output).
     * @param rhs Right-hand side vector of the system.
     * @param RCI_request Reverse Communication Interface request indicator.
     * @param ipar Integer parameter array for solver configuration.
     * @param dpar Double parameter array for solver configuration.
     * @param tmp Temporary workspace array.
     */
    public static native void dfgmresInit(int ivar, double[] computed_solution, double[] rhs, int[] RCI_request, int[] ipar, double[] dpar, double[] tmp);

    /**
     * Computes a sparse matrix-vector product in CSR format with two output vectors (double precision): y = A*x.
     * @param cvar Character specifying the operation type ('N' or 'T').
     * @param ivar Number of rows in the matrix.
     * @param A Array of non-zero values of the sparse matrix.
     * @param ia Row index array in CSR format.
     * @param ja Column index array in CSR format.
     * @param tmp Temporary/work array.
     * @param k Size parameter for the operation.
     * @param l Additional size parameter.
     */
    public static native void dcsrgemv2(char cvar, int ivar, double[] A, int[] ia, int[] ja, double[] tmp, int k, int l);

    /**
     * Computes a sparse matrix-vector product in CSR format (double precision): y = A*x.
     * @param cvar Character specifying the operation type ('N' or 'T').
     * @param ivar Number of rows in the matrix.
     * @param A Array of non-zero values of the sparse matrix.
     * @param ia Row index array in CSR format.
     * @param ja Column index array in CSR format.
     * @param x Input vector x.
     * @param y Output vector y receiving the result.
     */
    public static native void dcsrgemv(char cvar, int ivar, double[] A, int[] ia, int[] ja, double[] x, double[] y);

    /**
     * Performs one step of the FGMRES iterative solver via reverse communication.
     * @param n Size of the system (number of variables).
     * @param x Solution vector (updated on output).
     * @param b Right-hand side vector.
     * @param RCI_request Reverse Communication Interface request indicator.
     * @param ipar Integer parameter array for solver configuration.
     * @param dpar Double parameter array for solver configuration.
     * @param tmp Temporary workspace array.
     */
    public static native void dfgmres(int n, double[] x, double[] b, int[] RCI_request, int[] ipar, double[] dpar, double[] tmp);

    /**
     * Checks the FGMRES solver status for convergence or errors.
     * @param n Size of the system (number of variables).
     * @param x Current solution vector.
     * @param b Right-hand side vector.
     * @param RCI_request Reverse Communication Interface request indicator.
     * @param ipar Integer parameter array for solver configuration.
     * @param dpar Double parameter array for solver configuration.
     * @param tmp Temporary workspace array.
     */
    public static native void dfgmresCheck(int n, double[] x, double[] b, int[] RCI_request, int[] ipar, double[] dpar, double[] tmp);

    /**
     * Retrieves the FGMRES solution and iteration count after convergence.
     * @param ivar Size of the system (number of variables).
     * @param x Solution vector to be filled.
     * @param b Right-hand side vector.
     * @param RCI_request Reverse Communication Interface request indicator.
     * @param ipar Integer parameter array for solver configuration.
     * @param dpar Double parameter array for solver configuration.
     * @param tmp Temporary workspace array.
     * @param itercount Output array receiving the number of iterations performed.
     */
    public static native void dfgmresGet(int ivar, double[] x, double[] b, int[] RCI_request, int[] ipar, double[] dpar, double[] tmp, int[] itercount);
}
