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

package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import java.util.ArrayList;
import java.util.List;

/**
 * Optimized B-vector for circuit simulation that caches the stamps of
 * basis-stampable components. When no non-basis component has changed, the
 * cached basis stamp is simply copied instead of re-stamping all components.
 *
 * @author andy
 */
public class BVector {

    public double[] b;
    private double[] basisB;
    private final ArrayList<BStampable> _bStampables = new ArrayList<BStampable>();
    private final ArrayList<BStampable> _isBasisStampable = new ArrayList<BStampable>();
    private final ArrayList<BStampable> _isNotBasisStampables = new ArrayList<BStampable>();
    private boolean updateAllFlag = true;

    @SuppressWarnings("this-escape")
    public BVector(final int size, final List<BStampable> bstampables) {
        b = new double[size];
        basisB = new double[size];

        for (BStampable bstampable : bstampables) {
                _bStampables.add(bstampable);
                if (bstampable.isBasisStampable()) {
                    _isBasisStampable.add(bstampable);
                    bstampable.registerBVector(this);
                } else {
                    _isNotBasisStampables.add(bstampable);
                }
        }
    }

    //constructor for creating a copy of a BVector
    private BVector(double[] bcopy, double[] basisBcopy, ArrayList<BStampable> bStampables, ArrayList<BStampable> isBasisStampable, ArrayList<BStampable> isNotBasisStampable, boolean updateAllFlagcopy)
    {
        b = bcopy;
        basisB = basisBcopy;
        _bStampables.addAll(bStampables);
        _isBasisStampable.addAll(isBasisStampable);
        _isNotBasisStampables.addAll(isNotBasisStampable);
        updateAllFlag = updateAllFlagcopy;
    }


    /**
     * Stamps the B vector, reusing the cached basis stamp when possible.
     * Basis-stampable components are only re-stamped when {@link #setUpdateAllFlag()}
     * was called; non-basis components are always re-stamped.
     *
     * @param t the current simulation time
     * @param dt the current time step
     */
    public void stampBVector(double t, double dt) {
        //System.out.println("stamping B vectors at " + t + ", updateAllFlag: " + updateAllFlag);
        
   
        if (updateAllFlag) {
            for (int i = 0; i < b.length; i++) {
                basisB[i] = 0;
            }

            for (BStampable comp : _isBasisStampable) {
                comp.stampVectorB(basisB, t, dt);
            }

            for (int i = 0; i < b.length; i++) {
                b[i] = basisB[i];
            }

            for (BStampable comp : _isNotBasisStampables) {
                comp.stampVectorB(b, t, dt);
            }
            updateAllFlag = false;
            
        } else {
            for (int i = 0; i < b.length; i++) {
                b[i] = basisB[i];
            }

            for (BStampable comp : _isNotBasisStampables) {
                comp.stampVectorB(b, t, dt);
            }
        }
    }
    
    
    

    /**
     * Marks the basis stamp as dirty, forcing all basis-stampable components
     * to be re-stamped on the next {@link #stampBVector(double, double)} call.
     */
    public void setUpdateAllFlag() {
        updateAllFlag = true;
        //System.out.println("update all flag set to true: " + updateAllFlag);
    }

    /**
     * Creates a deep copy of this BVector, including arrays and component lists.
     *
     * @return a new BVector copy
     */
    public BVector copy()
    {
        double[] bCopy = new double[b.length];
        double[] basisBCopy = new double[basisB.length];
        System.arraycopy(b, 0, bCopy, 0, b.length);
        System.arraycopy(basisB, 0, basisBCopy, 0, basisB.length);
        BVector BCopy = new BVector(bCopy,basisBCopy,_bStampables,_isBasisStampable,_isNotBasisStampables,updateAllFlag);
        return BCopy;
    }
    
    /**
     * Re-registers this BVector with all basis-stampable components, used after
     * creating a copy so that components point to the new BVector instance.
     */
    public void registerBVector() {
        for (BStampable bstampable : _isBasisStampable) {
            bstampable.registerBVector(this);
        }
    }
}
