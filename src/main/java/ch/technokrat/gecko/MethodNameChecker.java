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
package ch.technokrat.gecko;

import java.lang.reflect.Method;

/**
 * pure utility class - no constructor. This utility function checks wheter the
 * methods of "checkMethods" are contained with the identical method signature
 * inside "containsMethodSignature".
 *
 * @author andy
 */
final class MethodNameChecker {

    private MethodNameChecker() {
        super();
    }

    /**
     * Validates that all methods defined in the {@code checkMethods} class/interface
     * are present with the identical signature in {@code containsMethodSignature}.
     * 
     * <p><strong>Assertion-based Execution Strategy:</strong>
     * This method uses a try-catch block around an {@code assert false} statement to
     * determine if JVM assertions are enabled (via the {@code -ea} flag). If assertions
     * are disabled (the default in production), the method returns immediately to save
     * startup/execution time. If assertions are enabled, the caught {@link AssertionError}
     * triggers the full reflection-based signature validation.
     * </p>
     *
     * @param checkMethods the class whose methods need to be checked
     * @param containsMethodSignature the interface that should contain matching methods
     * @return null
     */
    /**
     * Validates that all methods in {@code checkMethods} have an identical signature
     * in {@code containsMethodSignature}. Uses assertions so the check only runs
     * when the JVM is started with the {@code -ea} flag.
     * @param checkMethods the class whose methods should be checked
     * @param containsMethodSignature the interface expected to contain matching methods
     * @return always null
     */
    static MethodNameChecker checkFabric(final Class<?> checkMethods,
            final Class<GeckoRemoteInterface> containsMethodSignature) {
        try {
            assert false; // immediately return when assertions are turned off
            // we don't want to spend time in this check when users open GeckoCIRCUITS.
            return null;
        } catch (AssertionError err) { // we go here, when the JVM-flag "-ea" is set!
            for (Method toTest : checkMethods.getMethods()) {
                try {
                    assert containsMethodSignature.getMethod(toTest.getName(), toTest.getParameterTypes()) != null;                    
                } catch (Throwable ex) {
                    assert false : "Method in geckoRemoteInterface not found: " + toTest;
                }
            }
        }
        return null;
    }
}
