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

package gecko.geckocircuits.newscope;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Zimmi
 */
public class CompressedData {
    private static final Logger LOGGER = LogManager.getLogger(CompressedData.class);


    private final int _blockLength;
    private final int _maxDiff;
    private final int _bits;
    private final int[] exp = {1, 2, 4, 8, 16, 32, 64, 128, 256};
    private final byte[] _compressedData;
    private final double _compressRate;
    private final byte[] _differences;

    public CompressedData(byte[] compressableData) {

        _differences = new byte[compressableData.length - 1];
        for (int i = 0; i < compressableData.length - 1; i++) {
            _differences[i] = ((byte) (compressableData[i + 1] - compressableData[i]));
        }
        _blockLength = _differences.length;
        _maxDiff = evaluateMaxDifference(_differences);

        _bits = calculateNeededBits();

        final int resultLength = (int) Math.ceil((double) _blockLength * _bits / Byte.SIZE) + 2;
        _compressedData = new byte[resultLength];

        _compressedData[0] = compressableData[0];
        _compressedData[1] = ((byte) _bits);

        if (_maxDiff != 0) {
            compressData(_differences);
        }
        _compressRate = (((double) resultLength) / ((double) compressableData.length) * 100.0);

    }

    private byte trimByte(byte value, int length) {
        byte trimmer = (byte) (-1 << length);
        return (byte) ((value | trimmer) ^ trimmer);
    }

    private void compressData(byte[] compData) {
        int offset = 0;
        byte curByte = 0;
        int actualLength = Byte.SIZE;
        int curIndex = 2;
        for (int i = 0; i < _blockLength; i++) {
            // If not all of needed bits of the current byte have been added, add overhead bits to the next byte
            if (offset != 0) {
                actualLength = actualLength - offset;
                curByte = (byte) (curByte + (trimByte(compData[i - 1], _bits) << actualLength));
                offset = 0;
            }

            // Add needed bits to current Byte
            actualLength = actualLength - _bits;


            if (actualLength >= 0) {    // Case: enough space for bits
                curByte = (byte) (curByte + (trimByte(compData[i], _bits) << actualLength));
            } else {                    // Case: not all the bits can be added -> offset
                offset = Math.abs(actualLength);
                curByte = (byte) (curByte + (trimByte(compData[i], _bits) >> offset));
            }
            // Parameter reset & add compressed byte
            if (actualLength <= 0) {
                actualLength = Byte.SIZE;
                _compressedData[curIndex] = curByte;
                curIndex++;
                curByte = 0;
            }

        }

        // Finalize

        _compressedData[curIndex] = curByte;
        curIndex++;
        if (actualLength < 0) {
            curByte = 0;
            actualLength = Byte.SIZE + actualLength;
            curByte = (byte) (curByte + (trimByte(compData[compData.length - 1], _bits) << actualLength));
            _compressedData[curIndex] = curByte;
            curIndex++;
        }
    }

    private int evaluateMaxDifference(byte[] compData) {
        int maxDiff = 0;
        for (int i = 0; i < compData.length - 1; i++) {
            final byte diff = (byte) Math.abs(compData[i]);
            if (diff > maxDiff) {
                maxDiff = diff;
            }
        }
        return maxDiff;
    }

    private int calculateNeededBits() {
        for (int i = 0; i <= Byte.SIZE; i++) {
            if (2 * _maxDiff < exp[i]) {
                return i;
            }
        }
        return 0;
    }

    public int[] getEssentialData() {
        return new int[]{_bits, _blockLength + 1, _compressedData.length};
    }

    public void printCompressInfo() {
        LOGGER.info("\tcompression rate:\t" + _compressRate + "%");
        LOGGER.info("\tNeeded Bits:\t" + _bits);
        LOGGER.info("\tMaximal Difference:\t" + _maxDiff);
        LOGGER.info("\tLast byte:\t" + Integer.toBinaryString(_compressedData[_compressedData.length - 1] & 0xFF));
        LOGGER.info("\tLast Difference:\t" + Integer.toBinaryString(trimByte(_differences[_differences.length - 1], _bits)));
        LOGGER.info("\tresult length:\t" + _compressedData.length);
        LOGGER.info("\tblock length:\t" + (_blockLength + 1));

    }
}
