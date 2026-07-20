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
package gecko.geckocircuits.control.javablock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gecko.GeckoRuntimeException;
import gecko.geckocircuits.general.ProjectData;
import gecko.core.allg.GeckoFile;
import gecko.geckocircuits.circuit.SchematicEditor2;
import gecko.core.circuit.TokenMap;
import java.io.*;
import java.util.*;
public abstract class AbstractJavaBlock {
    private static final Logger LOGGER = LogManager.getLogger(AbstractJavaBlock.class);


    protected final ControlJavaFunction _controlJavaBlock;
    protected AbstractCompileObject _compileObject = new CompileObjectNull();
    JavaBlockSource _javaBlockSource = new JavaBlockSource.Builder().build();
    final List<GeckoFile> _additionalSourceFiles = new ArrayList<GeckoFile>();
    protected Map<String, CompiledClassContainer> _classNameFileMap;

    AbstractJavaBlock(final ControlJavaFunction controlJavaBlock) {
        _controlJavaBlock = controlJavaBlock;
    }

    abstract AbstractJavaBlock createOtherBlockTypeCopy();

    public CompileStatus getCompileStatus() {
        return _compileObject.getCompileStatus();
    }

    public abstract void findAndLoadClass();

    public void doCompilationIfRequired() throws IOException {
        if (!checkIfCompilationRequired()) {
            return;
        }
        SchematicEditor2.setZustandGeaendert(true);

        String className = CompileObject.findUniqueClassName();
        String sourceString = SourceFileGenerator.createSourceCode(_javaBlockSource, className, _controlJavaBlock.YOUT.size(), _controlJavaBlock._variableBusWidth);

        _compileObject = new CompileObject(sourceString, className, _additionalSourceFiles);

        if (_compileObject.getCompileStatus() == CompileStatus.COMPILED_SUCCESSFULL) {
            findAndLoadClass();
        }

        // repaint schematic entry - because color of JavaCode-Block could change
        SchematicEditor2.Singleton._circuitSheet.repaint();
    }

    private boolean checkIfCompilationRequired() {

        // test if the java block code changed from last compilation
        final String newSourceString = SourceFileGenerator.createSourceCode(_javaBlockSource,
                _compileObject.getClassName(), _controlJavaBlock.YOUT.size(),
                _controlJavaBlock._variableBusWidth);
        final String oldSourceString = _compileObject.getSourceCode();

        if (!newSourceString.equals(oldSourceString)) {
            return true;
        }

        // test if one of the external files changed:
        final Map<String, CompiledClassContainer> nameClassMap = _compileObject.getClassNameFileMap();

        final Set<String> compiledFileNames = nameClassMap.keySet();

        // first we check if any file was added / removed, by emptying the removeList:
        final Set<String> removeList = new TreeSet<String>();
        for (String fileName : compiledFileNames) {
            removeList.add(fileName + ".java");
        }

        removeList.remove(_compileObject.getClassName() + ".java");

        for (GeckoFile geckoFile : _additionalSourceFiles) {
            if (!removeList.contains(geckoFile.getName())) {
                return true; // a external file was newly added to the javablock... recompile required
            }
            removeList.remove(geckoFile.getName());
        }

        return !removeList.isEmpty(); // a external file was removed... recompile required
    }

    void compileNewBlockSource(final JavaBlockSource newSourceCode) {
        _javaBlockSource = newSourceCode;
        SchematicEditor2.Singleton.setDirtyFlag();

        try {
            doCompilationIfRequired();
        } catch (IOException ex) {
            LOGGER.error("IOException during compilation: " + ex.getMessage(), ex);
        }
    }

    public String getCompilerSource() {
        return _compileObject.getSourceCode();
    }

    public String getCompilerMessage() {
        return _compileObject.getCompilerMessage();
    }

    protected String getCompilationFailureDetails() {
        final StringBuilder details = new StringBuilder("Java block compilation/load failed");
        details.append(" (status: ").append(_compileObject.getCompileStatus()).append(")");

        final String compilerMessage = _compileObject.getCompilerMessage();
        if (compilerMessage != null && !compilerMessage.trim().isEmpty()) {
            details.append(". Compiler message:\n").append(compilerMessage.trim());
        }

        return details.toString();
    }

    JavaBlockSource getBlockSourceCode() {
        return _javaBlockSource;
    }

    void initialize(final double[][] inputSignals,
            final double[][] outputSignals) throws Exception {
        if (_compileObject.getCompileStatus() == CompileStatus.NOT_COMPILED) {
            doCompilationIfRequired();
        } else if (_compileObject.getCompileStatus() == CompileStatus.COMPILE_ERROR) {
            throw new GeckoRuntimeException("Could not compile Java-Block!");
        }

        doInitialize(inputSignals, outputSignals);
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
    abstract void calculateYOUT(final double time, final double deltaT, final double[][] inputSignals,
            final double[][] outputSignals) throws Exception;

    abstract void doInitialize(final double[][] xIN, final double[][] yOUT);

    void exportIndividualCONTROL(final StringBuffer ascii) {
        try {
            if (_compileObject.getCompileStatus() == CompileStatus.NOT_COMPILED) {
                doCompilationIfRequired();
            }
        } catch (Exception ex) {
            LOGGER.error("Could not find class during export", ex);
        }

        _javaBlockSource.exportIndividualCONTROL(ascii);

        ascii.append("\n<extraSourceFiles>");
        if (!_additionalSourceFiles.isEmpty()) {
            for (GeckoFile file : _additionalSourceFiles) {
                ascii.append('\n');
                ascii.append(file.getHashValue());
            }
        }
        ascii.append("\n<\\extraSourceFiles>");

        ProjectData.appendAsString(ascii.append("\nclassName"), _compileObject.getClassName());
        ProjectData.appendAsString(ascii.append("\nCompileStatus"), _compileObject.getCompileStatus().ordinal());

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream oOutStream = new ObjectOutputStream(baos);
            oOutStream.writeObject(_classNameFileMap);
            oOutStream.close();
            final byte[] outBytes = baos.toByteArray();
            ProjectData.appendAsString(ascii.append("\nclassMapBytes"), outBytes);
        } catch (IOException ex) {
            LOGGER.error("IOException while serializing class map: " + ex.getMessage(), ex);
        }

    }

    void importIndividualCONTROL(final TokenMap tokenMap) {
        _javaBlockSource = new JavaBlockSource(tokenMap);
        // Saved Java-block bytecode is not reused anymore. The source snippets in the
        // *.ipes file remain the authoritative representation and the block is compiled
        // again on first simulation initialize. Skipping the legacy classMapBytes[]
        // deserialization avoids noisy compatibility warnings for older project files.
        resetCompileObject();

    }

    void resetCompileObject() {
        _compileObject = new CompileObjectNull();
    }

    final void createNewJavaSourceCopy(AbstractJavaBlock returnValue) {
        returnValue._javaBlockSource = new JavaBlockSource.Builder().sourceCode(
                this._javaBlockSource._sourceCode).
                importsCode(this._javaBlockSource._importsCode).
                initCode(this._javaBlockSource._initCode).variablesCode(
                        this._javaBlockSource._variablesCode).
                build();
    }

}
