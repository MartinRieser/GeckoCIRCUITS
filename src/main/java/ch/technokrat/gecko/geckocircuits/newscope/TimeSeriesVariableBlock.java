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
package ch.technokrat.gecko.geckocircuits.newscope;

import ch.technokrat.gecko.geckocircuits.newscope.DataBlock.IndexLimit;
import ch.technokrat.gecko.geckocircuits.newscope.DataBlock.TimeLimit;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Block-based variable time series that optimizes memory by storing time-value pairs
 * in DataBlock segments. GetLastTimeInterval() throws UnsupportedOperationException
 * since the time step varies between blocks.
 */
public final class TimeSeriesVariableBlock extends AbstractTimeSeries {

    private DataBlock _lastBlock;
    private int _overallSize;
    private final SortedMap<IndexLimit, DataBlock> _indexData;
    private final SortedMap<TimeLimit, DataBlock> _timeData;

    public TimeSeriesVariableBlock() {
        super();
        _indexData = new TreeMap<IndexLimit, DataBlock>();
        _timeData = new TreeMap<TimeLimit, DataBlock>();
    }

    public int getNumBlocks() {
        if (_lastBlock != null) {
            return _indexData.size() + 1;
        }
        return 0;
    }

    @Override
    public double getValue(final int index) {
        final IndexLimit ind = new IndexLimit(index, index);
        DataBlock dataBlock = _indexData.get(ind);

        if (dataBlock == null) {
            dataBlock = _lastBlock;
        }
        return dataBlock.getBlockValue(index);
    }

    @Override
    public void setValue(final int index, final double value) {
        // you can only add values to the end!!
        assert index >= _overallSize;

        if (_lastBlock == null) {
            _lastBlock = new DataBlock(value, 0.0, 1, _overallSize);
        } else {
            if (!_lastBlock.setBlockValue(value)) {
                _indexData.put(_lastBlock.getIndexLimit(), _lastBlock);
                _timeData.put(new TimeLimit(_lastBlock.getStartValue(), value), _lastBlock);
                _lastBlock = new DataBlock(value, 0.0, 1, _overallSize);
            }
        }
        _overallSize++;
    }

    @Override
    public int getMaximumIndex() {
        return _overallSize - 1;
    }

    @Override
    public int findTimeIndex(final double time) {
        final TimeLimit timeLim = new TimeLimit(time, time);
        DataBlock dataBlock = _timeData.get(timeLim);

        if (dataBlock == null) {
            dataBlock = _lastBlock;
        }
        return dataBlock.findTimeIndex(time);
    }
    

    @Override
    public double getLastTimeInterval() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
