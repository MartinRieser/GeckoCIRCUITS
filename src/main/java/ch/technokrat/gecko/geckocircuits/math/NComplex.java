/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.math;

import ch.technokrat.gecko.geckocircuits.general.TechFormat;

/** Support for complex numbers in Java */
public class NComplex {
   private final float re;
   private final float im;
   private TechFormat tcf = new TechFormat();
   
   /**
    * Creates a new complex number with the given real and imaginary parts.
    * @param r the real part
    * @param i the imaginary part
    */
   public NComplex(float r, float i) {
      re = r;
      im = i;
   }

   /**
    * Creates a new complex number with the given real part and zero imaginary part.
    * @param r the real part (imaginary part is 0)
    */
   public NComplex(float r) {
      this(r, 0.0f);
   }

   /**
    * Creates a new complex number with both real and imaginary parts set to zero.
    */
   public NComplex() {
      this(0.0f, 0.0f);
   }

   /** returns real value of this */
   public float getRe() {
      return re;
   }

   /** returns imaginary value of this */
   public float getIm() {
      return im;
   }

   /** return a string representation of the complex number */
   public String toString()  { return re + " + " + im + "i"; }

   /**
    * Returns a new complex number with value a + b.
    * @param a first operand
    * @param b second operand
    * @return a + b
    */
   public static NComplex add(NComplex a, NComplex b) {
      float re, im;
      re = a.re + b.re;
      im = a.im + b.im;

      return new NComplex(re, im);
   }

   /**
    * Returns a new complex number with value a - b.
    * @param a first operand
    * @param b second operand
    * @return a - b
    */
   public static NComplex sub(NComplex a, NComplex b) {
      float re, im;
      re = a.re - b.re;
      im = a.im - b.im;

      return new NComplex(re, im);
   }

   /**
    * Returns a new complex number with value a * b.
    * @param a first operand
    * @param b second operand
    * @return a * b
    */
   public static NComplex mul(NComplex a, NComplex b) {
      float re, im;
      re = a.re*b.re - a.im*b.im;
      im = a.im*b.re + a.re*b.im;

      return new NComplex(re, im);
   }

   /**
    * Returns a new complex number with the conjugate value of a.
    * @param a the complex number to conjugate
    * @return complex conjugate of a
    */
   public static NComplex conj(NComplex a) {
      return new NComplex(a.re, -a.im);
   }

   /**
    * Returns a new complex number with value a / b.
    * @param a dividend
    * @param b divisor
    * @return a / b
    */
   public static NComplex div(NComplex a, NComplex b) {
      float re, im;
      float r,den;

      if (Math.abs(b.re) >= Math.abs(b.im)) {
         r = b.im/b.re;
         den = b.re+r*b.im;
         re = (a.re+r*a.im)/den;
         im = (a.im-r*a.re)/den;
      } else {
         r = b.re/b.im;
         den = b.im+r*b.re;
         re = (a.re*r+a.im)/den;
         im = (a.im*r-a.re)/den;
      }

      return new NComplex(re, im);
   }

   /**
    * Returns the absolute value (magnitude) of a.
    * @param a the complex number
    * @return sqrt(re^2 + im^2)
    */
   public static float abs(NComplex a) {
        if (a.re != 0.0f || a.im != 0.0f) {
            return (float)Math.sqrt(a.re*a.re + a.im*a.im);
        } else {
            return 0.0f;
        }
   }

   /**
    * Returns the square root of a.
    * @param a the complex number
    * @return sqrt(a)
    */
   public static NComplex sqrt(NComplex a) {
      float im,re;
      float x,y,w,r;

      if ((a.re == 0.0f) && (a.im == 0.0f)) {
         re = 0.0f;
         im = 0.0f;
      } else {
         x = Math.abs(a.re);
         y = Math.abs(a.im);

         if (x >= y) {
            r = y/x;
            w = (float)Math.sqrt(x)*(float)Math.sqrt(0.5f*(1.0f + Math.sqrt(1.0f + r*r)));
         } else {
            r = x/y;
            w = (float)Math.sqrt(y)*(float)Math.sqrt(0.5f*(r + Math.sqrt(1.0f + r*r)));
         }
         if (a.re >= 0.0) {
            re = w;
            im = a.im/(2.0f*w);
         } else {
            im = (a.im >= 0.0f) ? w : -w;
            re = a.im/(2.0f*im);
         }
      }

      return new NComplex(re, im);
   }

   /**
    * Returns a new complex number with value a * x (scalar multiplication).
    * @param x the scalar multiplier
    * @param a the complex number
    * @return a * x
    */
   public static NComplex multiplyByScalar(float x, NComplex a) {
      float im,re;

      re = x*a.re;
      im = x*a.im;

      return new NComplex(re, im);
   }

    public String nicePrint() {
        if(re != 0 && im != 0) {
            if(im > 0) {
                if(im == 1) {
                    return tcf.formatENG(re, 3) + "+i";
                } else 
                return tcf.formatENG(re, 3) + "+" + tcf.formatENG(im, 3) + "i";
            } else {
                if(im == -1) {
                    return tcf.formatENG(re, 3) + "-i";
                } else 
                return tcf.formatENG(re, 3) + ""  + tcf.formatENG(im, 3) + "i";
            }
        }
        
        if(re != 0) {
            return "" + tcf.formatENG(re, 3);
        } else {
            if(Math.abs(im) == 1) {
                if(im > 0) return "i";
                else return "-i";
            } else {
                return tcf.formatENG(im, 3) + "i";
            }
        }
    }

    @Override
    public int hashCode() {
        return Float.hashCode(re) - 7 * Float.hashCode(im) + 3;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof NComplex) {
            NComplex compare = (NComplex) o;
            if(compare.re == this.re && compare.im == this.im) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    
    
    
}
