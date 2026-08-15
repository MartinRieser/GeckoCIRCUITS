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
package gecko.geckocircuits.general;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.NetListContainer;
import gecko.geckocircuits.circuit.SchematicEditor2;
import gecko.geckocircuits.circuit.SimulationKernel;
import gecko.geckocircuits.circuit.SimulationKernel.SimulationStatus;
import gecko.geckocircuits.circuit.SolverSettings;
import gecko.geckocircuits.control.DataSaver;
import gecko.geckocircuits.control.NetzlisteCONTROL;
import gecko.geckocircuits.control.ControlOSZI;
import gecko.geckocircuits.datacontainer.ContainerStatus;
import gecko.geckoscript.SimulationAccess;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = {"PA_PUBLIC_PRIMITIVE_ATTRIBUTE", "EI_EXPOSE_REP2"},
        justification = "Public field for simulation kernel access; stores references for simulation coordination")
public final class SimulationRunner {
    private static final Logger LOGGER = LogManager.getLogger(SimulationRunner.class);

	private final SchematicEditor2 _se;
	private final SolverSettings _solverSettings;
	public SimulationKernel simKern;
	private NetListContainer nlContainer;

	private final List<SimulationStateListener> _simulationStateListeners = new CopyOnWriteArrayList<>();

	public SimulationRunner(final SchematicEditor2 schematicEntry, final SolverSettings solverSettings) {
		_se = schematicEntry;
		_solverSettings = solverSettings;
	}

	public void addSimulationStateListener(SimulationStateListener l) {
		_simulationStateListeners.add(l);
	}

	public void removeSimulationStateListener(SimulationStateListener l) {
		_simulationStateListeners.remove(l);
	}

	private void fireSimulationStarted() {
		for (SimulationStateListener l : _simulationStateListeners) {
			l.onSimulationStarted();
		}
	}

	private void fireSimulationPaused() {
		for (SimulationStateListener l : _simulationStateListeners) {
			l.onSimulationPaused();
		}
	}

	private void fireSimulationFinished(long elapsedTimeMs) {
		for (SimulationStateListener l : _simulationStateListeners) {
			l.onSimulationFinished(elapsedTimeMs);
		}
	}

	private void fireSimulationAborted(String errorMessage) {
		for (SimulationStateListener l : _simulationStateListeners) {
			l.onSimulationAborted(errorMessage);
		}
	}

	private void fireStatusUpdate(String message) {
		for (SimulationStateListener l : _simulationStateListeners) {
			l.onStatusUpdate(message);
		}
	}

	public void startCalculation(boolean createNewSimThread, SolverSettings solverSettings) throws Exception {
		boolean getAnfangsbedVomDialogfenster = true;
		fireSimulationStarted();

		simKern = new SimulationKernel();
		double tSTART = 0, tAktuell = tSTART;
		double tEND = solverSettings._tDURATION.getValue();
		double dtLoc = solverSettings.dt.getValue();

		if (solverSettings._T_pre.getValue() > 0) {
			solverSettings.inPreCalculationMode = true;
		}

		if (solverSettings.inPreCalculationMode) {
			tEND = solverSettings._T_pre.getValue();
			dtLoc = solverSettings._dt_pre.getValue();
		}

		nlContainer = NetListContainer.fabricStartSimulation(_se, simKern);

		simKern.initSimulation(
				dtLoc, tSTART, tAktuell, tEND, solverSettings._tPAUSE.getValue(),
				getAnfangsbedVomDialogfenster, nlContainer, false);
		simKern.setScopeMenuesStartStop();
		solverSettings._dt_ALT = dtLoc;
		simKern.initialisiereCONTROLatSimulationStart(dtLoc);  // not done when 'Continue' is enabled

		RunThreadRun _runThread = new RunThreadRun(solverSettings);

		if (createNewSimThread) {
			final Thread calc = new Thread(_runThread);
			calc.setName("simulationThread");
			calc.start();
		} else {
			_runThread.setRunWithoutThread();
			_runThread.run();
		}
	}

	public void continueCalculation(final boolean createNewSimThread, final SolverSettings solverSettings) throws Exception {
		boolean getAnfangsbedVomDialogfenster = false;
		fireSimulationStarted();

		double tAktuell = simKern.getCurrentTime();
		double tSTART = simKern.getTSTART();
		double tEND = simKern.getTEND();

		if (tAktuell >= tEND) {
			tSTART = tEND;
			tEND += solverSettings._tDURATION.getValue();
			simKern.setZeiten(tSTART, tEND, solverSettings.dt.getValue());
			boolean recalculateMatrix = false;
			if (solverSettings.dt.getValue() != solverSettings._dt_ALT) {
				recalculateMatrix = true;
			}
			solverSettings._dt_ALT = solverSettings.dt.getValue();

			nlContainer = NetListContainer.fabricContinueSimulation(_se, simKern, nlContainer);

			simKern.initSimulation(solverSettings.dt.getValue(), tSTART, tAktuell, tEND,
					solverSettings._tPAUSE.getValue(),
					getAnfangsbedVomDialogfenster, nlContainer, recalculateMatrix);
			simKern.setScopeMenuesStartStop();
			simKern.setInitialConditionsFromContinue();

		} else {
			simKern._simulationStatus = SimulationStatus.RUNNING;
			simKern.setScopeMenuesStartStop();
			NetzlisteCONTROL.globalData.setContainerStatus(ContainerStatus.RUNNING);
		}

		RunThreadRun runnable = new RunThreadRun(solverSettings);
		if (createNewSimThread) {
			new Thread(runnable).start();
		} else {
			runnable.run();
		}
	}

	private void waitForDataSavers() {
		int counter = 0;
		while (DataSaver.WAIT_COUNTER.get() != 0 && counter < 100) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {LogManager.getLogger(SimulationAccess.class).error("Exception occurred", ex);
			}
			counter++;
		}
	}

	public void pauseSimulation() {
		try {
			if (simKern != null) {
				simKern._simulationStatus = SimulationStatus.PAUSED;
			} else {
				return;
			}

			fireSimulationPaused();
			NetzlisteCONTROL.globalData.setContainerStatus(ContainerStatus.PAUSED);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void initSim(double dtLoc, double tEND) {
		// Ganz am Anfang: t0=0
		fireSimulationStarted();
		boolean getAnfangsbedVomDialogfenster = true;

		simKern = new SimulationKernel();
		double tSTART = 0, tAktuell = tSTART;
		_solverSettings._tDURATION.setValueWithoutUndo(tEND);
		_solverSettings.dt.setValueWithoutUndo(dtLoc);

		if (_solverSettings.inPreCalculationMode) {
			tEND = _solverSettings._T_pre.getValue();
			dtLoc = _solverSettings._dt_pre.getValue();
		}

		nlContainer = NetListContainer.fabricStartSimulation(_se, simKern);

		simKern.initSimulation(
				dtLoc, tSTART, tAktuell, tEND, _solverSettings._tPAUSE.getValue(),
				getAnfangsbedVomDialogfenster, nlContainer, false);
		_solverSettings._dt_ALT = dtLoc;
		simKern.initialisiereCONTROLatSimulationStart(dtLoc);
		fireStatusUpdate("Starting Simulation ... ");

		for (AbstractBlockInterface block : _se.getElementCONTROL()) {
			if (block instanceof ControlOSZI) {
				((ControlOSZI) block).setSimulationTimeBoundaries(simKern.getTSTART(), simKern.getTEND());
			}
		}
	}

	public void initSim() {
		this.initSim(0, 0); // dt and tEND should be set appropriately
	}

	private final class RunThreadRun implements Runnable {

		long q1;
		long q2;
		private final SolverSettings _settings;
		private boolean _runWithoutThread = false;

		public RunThreadRun(SolverSettings settings) {
			_settings = settings;
		}

		public void setRunWithoutThread() {
			_runWithoutThread = true;
		}

		@Override
		public void run() {
			try {
				for (AbstractBlockInterface block : _se.getElementCONTROL()) {
					if (block instanceof ControlOSZI) {
						((ControlOSZI) block).setSimulationTimeBoundaries(simKern.getTSTART(), simKern.getTEND());
					}
				}

				q1 = System.currentTimeMillis();
				q2 = 0;
				fireStatusUpdate("Starting Simulation ... ");

				try {
					simKern.runSimulation();
					// GUI: update menu items
				} catch (OutOfMemoryError err) {
					OutOfMemoryError error = new OutOfMemoryError("Could not allocate enough java RAM memory for the simulation!");
					error.initCause(err);
					throw error;
				} finally {
					if (!_settings.inPreCalculationMode) {
						endRun();
					} else {
						_settings.inPreCalculationMode = false;
						try {
							continueCalculation(false, _settings);
						} catch (Throwable error) {
							error.printStackTrace();
							throw new RuntimeException(error);
						}
					}
				}
			} catch (Throwable error) {
				pauseSimulation();
				simKern._simulationStatus = SimulationStatus.FINISHED;
				fireStatusUpdate("Simulation aborted.");
				if (!_runWithoutThread) {
					error.printStackTrace();
				} else {
					throw new RuntimeException(error);
				}
			}
		}

		public void endRun() {
			q2 = System.currentTimeMillis();
			pauseSimulation();
			simKern._simulationStatus = SimulationStatus.FINISHED;
			simKern.tearDownOnPause();
			waitForDataSavers();
			fireSimulationFinished(q2 - q1);
		}
	}

	public void external_init(double tEnd) {
		fireStatusUpdate("Starting Simulation ... ");
		boolean getAnfangsbedVomDialogfenster = true;
		simKern = new SimulationKernel();
		double tSTART = 0, tAktuell = tSTART;
		nlContainer = NetListContainer.fabricStartSimulation(_se, simKern);
		simKern.initSimulation(
				_solverSettings.dt.getValue(), tSTART, tAktuell, tEnd, _solverSettings._tPAUSE.getValue(),
				getAnfangsbedVomDialogfenster, nlContainer, false);
		simKern.initialisiereCONTROLatSimulationStart(_solverSettings.dt.getValue());
	}
}
