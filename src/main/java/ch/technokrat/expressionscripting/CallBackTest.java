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

package ch.technokrat.expressionscripting;


import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Test/demo class for JavaScript engine callback functionality. Demonstrates
 * how a Java object can be exposed to the JavaScript scripting engine and
 * invoked from script code.
 */
public class CallBackTest {

    /** The shared script engine manager for obtaining script engines. */
    private static ScriptEngineManager mgr = new ScriptEngineManager();
    /** The shared JavaScript script engine instance used for evaluation. */
    public static ScriptEngine engine = mgr.getEngineByName("JavaScript");
    
    /**
     * Entry point that registers this object as a callback named "callBack"
     * in the JavaScript engine, then evaluates a script that invokes it.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        try {
            engine.getContext().setAttribute("callBack", new CallBackTest(),
                    ScriptContext.ENGINE_SCOPE);
            
            
            String script = "callBack.invoke('some test')";
            engine.eval(script);
        } catch (ScriptException ex) {
            Logger.getLogger(CallBackTest.class.getName()).log(Level.SEVERE, null, ex);
        }
                    
            
            
    }
    
    /**
     * Callback method invoked from JavaScript to demonstrate Java-to-script
     * integration.
     *
     * @param test the string argument passed from the script
     * @throws ScriptException if a script error occurs
     */
    public void invoke(final String test) throws ScriptException {
    }
    
}
