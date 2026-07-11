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
package ch.technokrat.gecko.geckocircuits.control.javablock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for compile objects that manage Java source compilation for
 * Java-based control blocks. Tracks compiled class containers and their
 * compilation status.
 *
 * @author andreas
 */
public abstract class AbstractCompileObject {

    protected final Map<String, CompiledClassContainer> _classMap = new HashMap<String, CompiledClassContainer>();

    /**
     * @return the current compilation status
     */
    public abstract CompileStatus getCompileStatus();

    /**
     * Sets the error status when compilation fails.
     */
    abstract void setErrorStatus();

    /**
     * @return the compiler diagnostic message, if any
     */
    public abstract String getCompilerMessage();

    /**
     * @return the fully qualified name of the main compiled class
     */
    public abstract String getClassName();

    /**
     * @return the Java source code to be compiled
     */
    public abstract String getSourceCode();

    /**
     * @return an unmodifiable map of compiled class names to their byte-code containers
     */
    public final Map<String, CompiledClassContainer> getClassNameFileMap() {
        return Collections.unmodifiableMap(_classMap);
    }
}
