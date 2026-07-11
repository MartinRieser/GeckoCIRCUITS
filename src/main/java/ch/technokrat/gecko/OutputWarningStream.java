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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.JOptionPane;

/**
 * Monitors output volume during simulation and warns the user about excessive output,
 * which can slow down the simulation. Uses static counters shared across all instances
 * to track total output bytes globally.
 */
public final class OutputWarningStream extends BufferedOutputStream {

    private final PrintStream _ps;
    /** Whether the verbosity warning has already been shown for this instance. */
    private boolean _verbosityWarnShown = false;
    private static final long DEFAULT_WARN_SIZE = 50000000;
    /** Static threshold at which a warning is triggered (shared globally). */
    private static long warningBytesSize = DEFAULT_WARN_SIZE;
    /** Static byte counter accumulating total output across all instances. */
    private static long byteCounter = 0;
    /** Whether this stream writes to the original console output (true) or to an alternative buffer. */
    private boolean _isOriginalOutput = true;
    @SuppressWarnings("PMD")
    private StringBuffer _alternativeOutput;
    private static final int MAX_STRING_BUFFER_SIZE = 100000;
    private static String outputDescription;
    /** If true, no further warning dialogs will be shown. */
    private boolean _ignoreFutureMessages = false;
    private static final int BUFFER_FRACTION = 5; // this means, after cleaning the buffer, 1/5th of the original space is left
    private static final int SEARCH_NEWLINE_CHARS = 200;

    /**
     * Creates an OutputWarningStream wrapping the given output stream.
     *
     * @param aStream       the underlying output stream
     * @param bufferedWriter the print stream for buffered writing
     */
    public OutputWarningStream(final OutputStream aStream, final PrintStream bufferedWriter) {
        super(aStream);
        _ps = bufferedWriter;
    }

    /**
     * Writes bytes to the output, updates the byte counter, and checks for excessive output.
     *
     * @param bytes the data to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(final byte[] bytes) throws IOException {
        final String aString = new String(bytes);
        if (_isOriginalOutput) {
            _ps.append(aString);
        } else {
            _alternativeOutput.append(aString);
        }

        byteCounter += bytes.length;
        checkLineCount();
    }

    /**
     * Writes a sub-range of bytes to the output and checks for excessive output.
     *
     * @param bytes the data to write
     * @param off   the start offset in the data
     * @param len   the number of bytes to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(final byte[] bytes, final int off, final int len) throws IOException {
        final String aString = new String(bytes, off, len);
        byteCounter += bytes.length;
        if (_isOriginalOutput) {
            _ps.append(aString);
        } else {
            _alternativeOutput.append(aString);
            checkStringBufferSize();
        }
        checkLineCount();
    }

    /**
     * Checks if the accumulated byte count exceeds the warning threshold and shows a dialog if so.
     */
    public void checkLineCount() {
        if (byteCounter > warningBytesSize) {
            Thread showWarningThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    maybeShowWarning();
                }
            }) {
            };
            showWarningThread.start();

        }
    }

    /**
     * Resets the byte counter and warning flag.
     */
    public void reset() {
        byteCounter = 0;
        _verbosityWarnShown = false;
    }

    private void maybeShowWarning() {
        if (!_verbosityWarnShown && !_ignoreFutureMessages) {
            _verbosityWarnShown = true;
            String destination = "Console output";
            if (!_isOriginalOutput) {
                destination = "Block: " + outputDescription + "  (Text field)";
            }

            final Object[] options = {"Ok",
                "Ignore further messages"};


            //Custom button text
            final int selection = JOptionPane.showOptionDialog(null,
                    "Excessive usage of output messages during simulation detected! This slows down\n"
                    + "your simulation. Please check your simulation model for errors, or consider to\nreduce the"
                    + "verbosity of your custom Java-code!\n\nSource of output message: " + outputDescription + "\n"
                    + "Destination: " + destination,
                    "Performance Warning!",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (selection == 1) {
                _ignoreFutureMessages = true;
            }
        }
    }

    /**
     * Switches this stream to write to an alternative buffer instead of the original output.
     *
     * @param stringBuffer the alternative output buffer
     * @param description  a description of the output source
     */
    void setAlternativeOutput(final StringBuffer stringBuffer, final String description) {
        _isOriginalOutput = false;
        _alternativeOutput = stringBuffer;
        outputDescription = description;
    }

    /**
     * Switches this stream back to writing to the original console output.
     */
    void setOriginalOutput() {
        _isOriginalOutput = true;
    }

    private void checkStringBufferSize() {
        if (_alternativeOutput.length() > MAX_STRING_BUFFER_SIZE) {
            _alternativeOutput.delete(0, MAX_STRING_BUFFER_SIZE - MAX_STRING_BUFFER_SIZE / BUFFER_FRACTION);
            final int maxSearch = Math.min(_alternativeOutput.length(), SEARCH_NEWLINE_CHARS);
            final char[] searchForNewLine = new char[maxSearch];
            _alternativeOutput.getChars(0, maxSearch, searchForNewLine, 0);
            for (int index = 0; index < maxSearch; index++) {
                if (searchForNewLine[index] == '\n') {
                    _alternativeOutput.delete(0, index + 1);
                    break;
                }
            }
        }
    }

    /**
     * Restores console output mode and updates the output description.
     *
     * @param description a description of the output source
     */
    void setConsoleOutput(final String description) {
        _isOriginalOutput = true;
        outputDescription = description;
    }
}
