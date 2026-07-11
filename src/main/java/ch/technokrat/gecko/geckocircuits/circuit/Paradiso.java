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

import com.intel.mkl.LAPACK;
import java.util.logging.Level;
import java.util.logging.Logger;




/**
 * Wrapper for the Intel PARDISO sparse direct solver. Provides factorize and solve
 * methods for symmetric/non-symmetric systems with configurable matrix type (mtype).
 *
 * @author muesinga
 */
public class Paradiso {

    
    private final static int nrhs = 1;
    private final static int idum = 0;              /* Integer dummy. */
    /* Pardiso control parameters. */
    public int[] iparm = new int[64];

    private static final int IPARM_SOLVER_DEFAULT = 0;
    private static final int IPARM_FILL_IN_REORDERING = 1;
    private static final int IPARM_NUM_PROCESSORS = 2;
    private static final int IPARM_ITERATIVE_DIRECT = 3;
    private static final int IPARM_USER_FILL_IN_PERM = 4;
    private static final int IPARM_WRITE_SOLUTION = 5;
    private static final int IPARM_NOT_IN_USE_6 = 6;
    private static final int IPARM_MAX_ITER_REF_STEPS = 7;
    private static final int IPARM_NOT_IN_USE_8 = 8;
    private static final int IPARM_PERTURB_PIVOT = 9;
    private static final int IPARM_NONSYM_PERM_SCALING = 10;
    private static final int IPARM_NOT_IN_USE_11 = 11;
    private static final int IPARM_MAX_WEIGHTED_MATCH = 12;
    private static final int IPARM_OUTPUT_PERTURBED = 13;
    private static final int IPARM_NOT_IN_USE_14 = 14;
    private static final int IPARM_NOT_IN_USE_15 = 15;
    private static final int IPARM_NOT_IN_USE_16 = 16;
    private static final int IPARM_OUTPUT_NONZEROS_LU = 17;
    private static final int IPARM_OUTPUT_MFLOPS = 18;
    private static final int IPARM_OUTPUT_CG_ITER = 19;
    private static final int IPARM_EPS_PIVOT = 28;

    /* Internal solver memory pointer pt,                  */
        /* 32-bit: int pt[64]; 64-bit: long int pt[64]         */
        /* or void *pt[64] should be OK on both architectures  */
    public int[] pt = new int[64];
    private static int mnum;
    private static int maxfct;
    private static int msglvl;


    public Paradiso() {
        
    }
    
   
    
    /**
     * Performs symbolic and numerical factorization of the sparse matrix using PARDISO.
     * mtype values: 1 = structurally symmetric, 2 = symmetric positive definite,
     * -2 = symmetric indefinite, 11 = non-symmetric, 13 = complex.
     *
     * @param values  matrix non-zero values
     * @param ai      row indices (CSR format)
     * @param aj      column pointers (CSR format)
     * @param n       matrix dimension
     * @param mtype   matrix type indicator
     * @param paradiso the Paradiso instance holding solver state
     */
    public static void factorize(double[] values, int[] ai, int[] aj, int n, int mtype, Paradiso paradiso) {
        /* Auxiliary variables. */

        double[] ddum = new double[2];        /* Double dummy */
        

        /* -------------------------------------------------------------------- */
        /* .. Setup Pardiso control parameters. */
        /* -------------------------------------------------------------------- */
        for (int i = 0; i < 64; i++) {
            paradiso.iparm[i] = 0;
        }
        paradiso.iparm[IPARM_SOLVER_DEFAULT] = 1; /* No solver default */
        paradiso.iparm[IPARM_FILL_IN_REORDERING] = 2; /* Fill-in reordering from METIS */
                        
        paradiso.iparm[IPARM_NUM_PROCESSORS] = 4; /* Numbers of processors, value of OMP_NUM_THREADS */
        paradiso.iparm[IPARM_ITERATIVE_DIRECT] = 0; /* 0No iterative-direct algorithm */
        paradiso.iparm[IPARM_USER_FILL_IN_PERM] = 0; /* No user fill-in reducing permutation */
        paradiso.iparm[IPARM_WRITE_SOLUTION] = 0; /* Write solution into x */
        paradiso.iparm[IPARM_NOT_IN_USE_6] = 0; /* Not in use */
        paradiso.iparm[IPARM_MAX_ITER_REF_STEPS] = 2; /* Max numbers of iterative refinement steps */
        paradiso.iparm[IPARM_NOT_IN_USE_8] = 0; /* Not in use */
        paradiso.iparm[IPARM_PERTURB_PIVOT] = 13; /* Perturb the pivot elements with 1E-13 */
        paradiso.iparm[IPARM_NONSYM_PERM_SCALING] = 1; /* Use nonsymmetric permutation and scaling MPS */
        paradiso.iparm[IPARM_NOT_IN_USE_11] = 0; /* Not in use */
        paradiso.iparm[IPARM_MAX_WEIGHTED_MATCH] = 0; /* Maximum weighted matching algorithm is switched-on (default for non-symmetric) */
        paradiso.iparm[IPARM_OUTPUT_PERTURBED] = 0; /* Output: Number of perturbed pivots */
        paradiso.iparm[IPARM_NOT_IN_USE_14] = 0; /* Not in use */
        paradiso.iparm[IPARM_NOT_IN_USE_15] = 0; /* Not in use */
        paradiso.iparm[IPARM_NOT_IN_USE_16] = 0; /* Not in use */
        paradiso.iparm[IPARM_OUTPUT_NONZEROS_LU] = -1; /* Output: Number of nonzeros in the factor LU */
        paradiso.iparm[IPARM_OUTPUT_MFLOPS] = -1; /* Output: Mflops for LU factorization */
        paradiso.iparm[IPARM_OUTPUT_CG_ITER] = 0; /* Output: Numbers of CG Iterations */
        paradiso.iparm[IPARM_EPS_PIVOT] = 1;
        maxfct = 1;         /* Maximum number of numerical factorizations.  */
        mnum = 1;         /* Which factorization to use. */
        
        msglvl = 0;         /* Print statistical information  */
        int error = 0;         /* Initialize error flag */

        /* -------------------------------------------------------------------- */
        /* .. Initialize the internal solver memory pointer. This is only */
        /* necessary for the FIRST call of the PARDISO solver. */
        /* -------------------------------------------------------------------- */
        for (int i = 0; i < 64; i++) {
            paradiso.pt[i] = 0;
        }
        
        
        /* -------------------------------------------------------------------- */
        /*.. Reordering and Symbolic Factorization.  This step also allocates*/
        /*     all memory that is necessary for the factorization.              */
        /* -------------------------------------------------------------------- */
        int phase = 11;        
        
        //long facstart = System.currentTimeMillis();        
        LAPACK.PARDISO(paradiso.pt, maxfct, mnum, mtype, phase,
                n, values, ai, aj, idum, nrhs,
                paradiso.iparm, msglvl, ddum, ddum, error);

        if (error != 0) {
                Logger.getLogger(Paradiso.class.getName()).log(Level.SEVERE, "\nERROR during symbolic factorization: " + error);
        }

        /* -------------------------------------------------------------------- */
        /* ..  Numerical factorization.                                         */
        /* -------------------------------------------------------------------- */
        phase = 22;
        LAPACK.PARDISO(paradiso.pt, maxfct, mnum, mtype, phase,
                n, values, ai, aj, idum, nrhs,
                paradiso.iparm, msglvl, ddum, ddum, error);

        if (error != 0) {
            Logger.getLogger(Paradiso.class.getName()).log(Level.SEVERE, "ERROR during numerical factorization: " + error);
        }
    }
    
    
    /**
     * Solves the factored linear system Ax = b using PARDISO back-substitution.
     *
     * @param values  matrix non-zero values
     * @param ai      row indices (CSR format)
     * @param aj      column pointers (CSR format)
     * @param rhs     right-hand side vector(s)
     * @param n       matrix dimension
     * @param mtype   matrix type (same as used in factorize)
     * @param nRHS    number of right-hand sides
     * @param paradiso the Paradiso instance with factored matrix
     * @return the solution vector(s)
     */
    public static double[] solve(double[] values, int[] ai, int[] aj, double[] rhs, int n, int mtype, int nRHS, Paradiso paradiso) {
        /* RHS and solution vectors. */
        double[] x = null;
        if(mtype == 13) {
             x = new double[2 * n * nRHS];
        } else {
            x = new double[n * nRHS];
        }
        

        /* /\* -------------------------------------------------------------------- *\/     */
        /* /\* ..  Back substitution and iterative refinement.                      *\/ */
        /* -------------------------------------------------------------------- */
        int phase = 33;
        int error = 0;
        
        
        LAPACK.PARDISO(paradiso.pt, maxfct, mnum, mtype, phase,
                n, values, ai, aj, idum, nRHS,
                paradiso.iparm, msglvl, rhs, x, error);


        if (error != 0) {
            Logger.getLogger(Paradiso.class.getName()).log(Level.SEVERE, "\nERROR during solution: " + error);
        }
                
        /* -------------------------------------------------------------------- */
        /* ..  Termination and release of memory.                               */
        /* -------------------------------------------------------------------- */
        //phase = -1;                 /* Release internal memory. */
        /* Release internal memory. */ 
        //int dummy = LAPACK.PARDISO(pt, maxfct, mnum, mtype, phase, n, ddum, ai, aj, idum, nrhs, iparm, msglvl, ddum, ddum, error);       
        return x;
    }
    
}
