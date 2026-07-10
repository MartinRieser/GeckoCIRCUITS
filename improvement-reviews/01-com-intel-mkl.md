# Improvement Tasks: com/intel/mkl/

## LAPACK.java
- Remove the stale IDE template comment on lines 2-4 ("To change this template, choose Tools | Templates...")
- Fix incorrect Javadoc on `zgetrs()` that says "Wrapper for MKL function dgetrf()" instead of zgetrs
- Fix incorrect Javadoc on `zgetrs2()` that says "Wrapper for MKL function dgetrf()" instead of zgetrs2
- Fix incorrect Javadoc on `cgetrs()` that says "Wrapper for MKL function dgetrf()" instead of cgetrs
- Fix incorrect Javadoc on `sgetrs()` that says "Wrapper for MKL function dgetrf()" instead of sgetrs
- Fix incorrect Javadoc on `dgetrs()` that says "Wrapper for MKL function dgetrf()" instead of dgetrs
- Fix incorrect Javadoc on `zsytrf()` that says "Wrapper for MKL function dgetrf()" instead of zsytrf
- Fix incorrect Javadoc on `zsytrs()` that says "Wrapper for MKL function dgetrf()" instead of zsytrs
- Fix incorrect Javadoc on `zsptrf()` that says "Wrapper for MKL function dgetrf()" instead of zsptrf
- Fix incorrect Javadoc on `zsptrs()` that says "Wrapper for MKL function zgetrs()" instead of zsptrs
- Fix incorrect Javadoc on `csptrs()` that says "Wrapper for MKL function zgetrs()" instead of csptrs
- Add Javadoc with `@param`/`@return` documentation for `spotri()`, `spptrf()`, `spptri()`, `spptrf2()`, `spptri2()`, `cpptri()`, `cpptrf()`, `cgetrf()`, `csptrf()`, `csptri()` which have none
- Add Javadoc with `@param`/`@return` for `PARDISO()` which has many unclear parameters (maxfct, mnum, mtype, phase, idum, etc.)
- Add Javadoc for `sgecon()`, `dgecon()`, `zgecon()` which have none
- Add Javadoc for `zgeequ()`, `zgeequ2()`, `zlaqge()`, `claqge()`, `cgeequ()` which have none
- Fix mismatched `@param` tags in `csprfs()` Javadoc: documents `@param a` but parameter is `af`, and `afp` parameter has no `@param` tag
- Fix mismatched `@param` tags in `zsprfs()` Javadoc: same issue
- Complete the empty `@return` tags in `sgetri()`, `dgetri()`, and `zgetri()` Javadoc

## LAPACKNative.java
- Remove the stale IDE template comment on lines 2-4
- Remove commented-out dead code on line 27: `//System.loadLibrary( "mkl_java_stubs" );`
- Add Javadoc to the class explaining it is package-private and holds JNI native method declarations
- Add Javadoc to the static initializer block explaining the platform-specific library loading logic
- Add Javadoc to the `PARADISO()` native method documenting all parameters and return value
- Add Javadoc to all other native method declarations (~50 methods have no Javadoc)
- Add Javadoc to grouped method sections explaining the s/d/c/z naming convention (single/double/complex/double-complex)
- Remove or document the developer marker comment `// ----- andy ----` on line 54

## CBLAS.java
- Add class-level Javadoc noting this class uses native methods directly
- Add `@param` documentation to `sgemm()`, `dgemm()`, `cgemm()`, `zgemm()` which only have one-line descriptions
- Add `@param` documentation to `sdot()`, `ddot()`, `cdotc_sub()`, `zdotc_sub()`, `cdotu_sub()`, `zdotu_sub()` which only have one-line descriptions
- Add full Javadoc to `ddnscsr()` which has no documentation at all
- Add full Javadoc to `dcsrmm()`, `scsrmm()`, `zcsrmm()` which have no documentation
- Fix `ccsrmm()` Javadoc: all `@param` tags are empty (just parameter names, no descriptions)
- Fix incorrect Javadoc on `cspmv()`: describes full `y := alpha*A*x + beta*y` but actual method signature is only `cspmv(int n, float[] ap, float[] x, float[] y)` -- copy-paste error from `sspmv`
- Add Javadoc to `dnrm2()` which has no documentation and uses unclear parameter names
- Add Javadoc to `dfgmresInit()` which has no documentation and unclear parameters
- Add Javadoc to `dcsrgemv2()` which has no documentation
- Add Javadoc to `dcsrgemv()` which has no documentation
- Add Javadoc to `dfgmres()` which has no documentation
- Add Javadoc to `dfgmresCheck()` which has no documentation
- Add Javadoc to `dfgmresGet()` which has no documentation
- Remove commented-out dead code on line 227
