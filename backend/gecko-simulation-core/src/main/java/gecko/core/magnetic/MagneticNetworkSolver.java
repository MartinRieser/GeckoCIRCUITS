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
package gecko.core.magnetic;

import gecko.core.allg.SolverType;
import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.circuit.parameters.SourceParameters;
import gecko.core.simulation.solver.MnaSolver;
import gecko.core.simulation.solver.MatrixSolver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated MNA solver for magnetic reluctance networks, computed as a
 * co-simulation domain next to the electrical circuit.
 *
 * <p>The magnetic network is assembled from the classic GeckoCIRCUITS magnetic
 * element types and solved by the same {@link MnaSolver} machinery as the
 * electrical domain, with the permeance analogy 1:1 between the domains:
 * the node potentials are the scalar magnetic potentials (MMF) in
 * ampere-turns, the branch "currents" are the magnetic fluxes in webers,
 * reluctances stamp as conductances with their permeance, and windings stamp
 * as REL_MMF sources enforcing {@code F_A - F_B = N * i} (Ampere's law).
 *
 * <p>Nonlinear core branches are converged per time step by Newton-Raphson
 * iteration on the differential permeance {@code P_diff = dPhi/dF}: each
 * iteration re-stamps the branch with its tangent permeance plus the Newton
 * companion current {@code Phi(F_k) - P_diff(F_k) * F_k} (an LK_I offset
 * source) and re-solves, so the fixed point is the exact solution of the
 * nonlinear network; after convergence the branch flux is evaluated from the
 * exact saturation curve. The solver is algebraic (no dynamic elements), so the magnetic time
 * dependence enters only through the winding currents and the Faraday EMF
 * {@code v = N * dPhi/dt}, which is evaluated from the flux difference of
 * consecutive steps.
 *
 * <p>Sign conventions: a positive winding current drives the flux from
 * {@code nodeA} through the external magnetic circuit to {@code nodeB}; the
 * winding flux readout is positive in that direction and the induced EMF is
 * {@code v = N * dPhi/dt} of that flux. Node MMFs read relative to the
 * reference node pinned per magnetic island.
 *
 * <p>Branches and windings must be added before the first
 * {@link #step(double, double)} call; the network topology is fixed afterwards
 * so the flux history stays consistent.
 *
 * @see MagneticWinding
 * @see ReluctanceBranch
 * @see NonlinearReluctance
 * @since v2.18.0 Task L1 - Multi-Domain Coupling (Step 3: Magnetic Domain Engine)
 */
public final class MagneticNetworkSolver {

    /** Source type of the REL_MMF winding sources. */
    private static final double DC_SOURCE_TYPE = SourceType.QUELLE_DC_NEW;

    /** Number of parameter slots of the assembled REL_MMF source elements. */
    private static final int MMF_SOURCE_PARAM_SLOTS = 2;

    /** Maximum Newton-Raphson iterations per time step. */
    private static final int MAX_NEWTON_ITERATIONS = 100;

    /** Absolute Newton tolerance on the branch MMF drop in ampere-turns. */
    private static final double NEWTON_ABSOLUTE_TOLERANCE = 1e-9;

    /** Relative Newton tolerance on the branch MMF drop. */
    private static final double NEWTON_RELATIVE_TOLERANCE = 1e-9;

    private final MnaSolver matrixSolver;
    private final SolverType solverType;

    private final List<LinearBranch> linearBranches = new ArrayList<>();
    private final List<NonlinearBranch> nonlinearBranches = new ArrayList<>();
    private final Map<String, WindingRuntime> windings = new LinkedHashMap<>();
    private final Map<String, Integer> branchNameGuard = new HashMap<>();

    private CircuitNetlist netlist;
    private boolean matricesInitialized;
    private int maxNodeIndex = -1;
    private double lastStepWidth;

    /**
     * Creates a magnetic network solver.
     *
     * @param solverType integration method of the co-simulation (the magnetic
     *        network itself is algebraic; the type is shared for consistency
     *        with the electrical and thermal domains)
     *
     * @throws IllegalArgumentException if solverType is null
     */
    public MagneticNetworkSolver(final SolverType solverType) {
        if (solverType == null) {
            throw new IllegalArgumentException("Solver type must not be null");
        }
        this.solverType = solverType;
        this.matrixSolver = new MatrixSolver(solverType);
    }

    /**
     * Adds a linear reluctance branch between two magnetic nodes.
     *
     * @param name unique branch name
     * @param nodeA first magnetic node
     * @param nodeB second magnetic node
     * @param branch linear reluctance of the branch
     *
     * @throws IllegalStateException if the network already stepped
     * @throws IllegalArgumentException if the name is null/blank/duplicate or
     *         a node or the branch is null
     */
    public void addReluctance(final String name, final MagneticNode nodeA, final MagneticNode nodeB,
                              final ReluctanceBranch branch) {
        requireModifiable();
        requireFreeBranchName(name);
        if (nodeA == null || nodeB == null || branch == null) {
            throw new IllegalArgumentException("Branch nodes and reluctance must not be null");
        }
        linearBranches.add(new LinearBranch(name, nodeA.value(), nodeB.value(),
                branch.getReluctance()));
        branchNameGuard.put(name, linearBranches.size() + nonlinearBranches.size());
        maxNodeIndex = Math.max(maxNodeIndex, Math.max(nodeA.value(), nodeB.value()));
    }

    /**
     * Adds a nonlinear (saturable) reluctance branch between two magnetic
     * nodes; its permeance stamp is converged by Newton-Raphson iteration.
     *
     * @param name unique branch name
     * @param nodeA first magnetic node
     * @param nodeB second magnetic node
     * @param model saturation characteristic of the branch
     *
     * @throws IllegalStateException if the network already stepped
     * @throws IllegalArgumentException if the name is null/blank/duplicate or
     *         a node or the model is null
     */
    public void addNonlinearReluctance(final String name, final MagneticNode nodeA,
                                       final MagneticNode nodeB, final NonlinearReluctance model) {
        requireModifiable();
        requireFreeBranchName(name);
        if (nodeA == null || nodeB == null || model == null) {
            throw new IllegalArgumentException("Branch nodes and saturation model must not be null");
        }
        nonlinearBranches.add(new NonlinearBranch(name, nodeA.value(), nodeB.value(), model));
        branchNameGuard.put(name, linearBranches.size() + nonlinearBranches.size());
        maxNodeIndex = Math.max(maxNodeIndex, Math.max(nodeA.value(), nodeB.value()));
    }

    /**
     * Adds a winding coupled between two magnetic nodes. The winding stamps a
     * REL_MMF source enforcing {@code F_nodeA - F_nodeB = N * i}; its current
     * is set via {@link #setWindingCurrent(String, double)} (default 0 A) and
     * its flux and induced EMF are read back after each step.
     *
     * @param winding winding descriptor (name, turns, nodes)
     *
     * @throws IllegalStateException if the network already stepped
     * @throws IllegalArgumentException if the winding is null or its name
     *         duplicates a winding name
     */
    public void addWinding(final MagneticWinding winding) {
        requireModifiable();
        if (winding == null) {
            throw new IllegalArgumentException("Winding must not be null");
        }
        if (windings.containsKey(winding.getName()) || branchNameGuard.containsKey(winding.getName())) {
            throw new IllegalArgumentException("Winding '" + winding.getName()
                    + "' is already registered");
        }
        windings.put(winding.getName(), new WindingRuntime(winding));
        maxNodeIndex = Math.max(maxNodeIndex, Math.max(winding.getNodeA().value(),
                winding.getNodeB().value()));
    }

    /**
     * Sets the electrical current of a winding for the upcoming time step.
     *
     * @param name winding name
     * @param currentAmpere winding current in amperes
     *
     * @throws IllegalArgumentException if the winding name is unknown or the
     *         current is not finite
     */
    public void setWindingCurrent(final String name, final double currentAmpere) {
        final WindingRuntime winding = windings.get(name);
        if (winding == null) {
            throw new IllegalArgumentException("Unknown winding: " + name);
        }
        if (!Double.isFinite(currentAmpere)) {
            throw new IllegalArgumentException("Winding current must be finite, got: "
                    + currentAmpere);
        }
        winding.current = currentAmpere;
        if (matricesInitialized) {
            netlist.getParameter(winding.elementIndex)[SourceParameters.INDEX_VALUE_DC] =
                    winding.turns * currentAmpere;
        }
    }

    /**
     * Performs one magnetic time step: drives the winding MMFs, converges the
     * nonlinear branches by Newton-Raphson iteration on the differential
     * permeance and evaluates the winding fluxes and induced EMFs. The
     * network is algebraic, so the step width enters only the EMF difference
     * quotient.
     *
     * @param dt time step width in seconds, positive
     * @param time current simulation time in seconds
     *
     * @throws IllegalArgumentException if dt is not finite or not positive
     * @throws IllegalStateException if the Newton-Raphson iteration does not converge
     */
    public void step(final double dt, final double time) {
        if (!Double.isFinite(dt) || dt <= 0.0) {
            throw new IllegalArgumentException("Time step must be finite and positive, got: " + dt);
        }
        initializeIfRequired();

        // Flux of the previous step (the potential vector still holds it)
        for (final WindingRuntime winding : windings.values()) {
            winding.previousFlux = -matrixSolver.getP()[winding.zRow];
        }

        for (int iteration = 0; iteration < MAX_NEWTON_ITERATIONS; iteration++) {
            // Newton companion of the nonlinear branches: tangent permeance
            // stamp plus the offset current Phi(F_k) - P_diff(F_k) * F_k,
            // carried by an LK_I source from nodeX to nodeY
            for (final NonlinearBranch branch : nonlinearBranches) {
                final double tangentPermeance =
                        branch.model.differentialPermeance(branch.mmfdropIterate);
                final double offset = branch.model.flux(branch.mmfdropIterate)
                        - tangentPermeance * branch.mmfdropIterate;
                netlist.getParameter(branch.elementIndex)[0] = 1.0 / tangentPermeance;
                netlist.getParameter(branch.offsetElementIndex)[SourceParameters.INDEX_VALUE_DC] =
                        offset;
            }
            matrixSolver.buildMatrixA(netlist, dt, time, false);
            matrixSolver.buildVectorB(netlist, dt, time, false);
            matrixSolver.solve();

            boolean converged = true;
            for (final NonlinearBranch branch : nonlinearBranches) {
                final double mmfDrop = matrixSolver.getP()[branch.nodeX]
                        - matrixSolver.getP()[branch.nodeY];
                final double tolerance = Math.max(NEWTON_ABSOLUTE_TOLERANCE,
                        NEWTON_RELATIVE_TOLERANCE * Math.abs(mmfDrop));
                if (Math.abs(mmfDrop - branch.mmfdropIterate) > tolerance) {
                    converged = false;
                }
                branch.mmfdropIterate = mmfDrop;
                branch.flux = branch.model.flux(mmfDrop);
            }
            if (converged) {
                finalizeStep(dt);
                return;
            }
        }
        throw new IllegalStateException("Magnetic Newton-Raphson iteration did not converge within "
                + MAX_NEWTON_ITERATIONS + " iterations at t=" + time);
    }

    /**
     * Evaluates the winding fluxes and induced EMFs after a converged solve.
     *
     * @param dt accepted time step width in seconds
     */
    private void finalizeStep(final double dt) {
        lastStepWidth = dt;
        for (final WindingRuntime winding : windings.values()) {
            winding.flux = -matrixSolver.getP()[winding.zRow];
            winding.inducedVoltage = winding.turns * (winding.flux - winding.previousFlux) / dt;
        }
    }

    /**
     * Assembles the magnetic netlist and initializes the MNA matrices on the
     * first step (all branches and windings must be registered by then).
     */
    private void initializeIfRequired() {
        if (matricesInitialized) {
            return;
        }
        final int elementCount = linearBranches.size() + 2 * nonlinearBranches.size()
                + windings.size();
        final CircuitTypCore[] types = new CircuitTypCore[elementCount];
        final List<Integer> nodeX = new ArrayList<>();
        final List<Integer> nodeY = new ArrayList<>();
        final int[] voltageSourceNumbers = new int[elementCount];
        final double[][] params = new double[elementCount][];

        int nodeMax = 0;
        int elementIndex = 0;
        for (final LinearBranch branch : linearBranches) {
            types[elementIndex] = CircuitTypCore.REL_RELUCTANCE;
            nodeX.add(branch.nodeX);
            nodeY.add(branch.nodeY);
            voltageSourceNumbers[elementIndex] = -1;
            params[elementIndex] = new double[]{branch.reluctance};
            nodeMax = Math.max(nodeMax, Math.max(branch.nodeX, branch.nodeY));
            elementIndex++;
        }
        for (final NonlinearBranch branch : nonlinearBranches) {
            types[elementIndex] = CircuitTypCore.REL_RELUCTANCE;
            nodeX.add(branch.nodeX);
            nodeY.add(branch.nodeY);
            voltageSourceNumbers[elementIndex] = -1;
            // initial stamp: tangent at the zero iterate = unsaturated permeance
            params[elementIndex] = new double[]{
                    1.0 / branch.model.differentialPermeance(branch.mmfdropIterate)};
            nodeMax = Math.max(nodeMax, Math.max(branch.nodeX, branch.nodeY));
            branch.elementIndex = elementIndex;
            elementIndex++;
            // Newton companion offset current (updated per iteration)
            types[elementIndex] = CircuitTypCore.LK_I;
            nodeX.add(branch.nodeX);
            nodeY.add(branch.nodeY);
            voltageSourceNumbers[elementIndex] = -1;
            final double[] offsetParams = new double[]{DC_SOURCE_TYPE, 0.0};
            params[elementIndex] = offsetParams;
            branch.offsetElementIndex = elementIndex;
            elementIndex++;
        }
        int windingSourceNumber = 0;
        for (final WindingRuntime winding : windings.values()) {
            types[elementIndex] = CircuitTypCore.REL_MMF;
            nodeX.add(winding.nodeA);
            nodeY.add(winding.nodeB);
            voltageSourceNumbers[elementIndex] = ++windingSourceNumber;
            final double[] sourceParams = new double[MMF_SOURCE_PARAM_SLOTS];
            sourceParams[SourceParameters.INDEX_SOURCE_TYPE] = DC_SOURCE_TYPE;
            sourceParams[SourceParameters.INDEX_VALUE_DC] = winding.turns * winding.current;
            params[elementIndex] = sourceParams;
            nodeMax = Math.max(nodeMax, Math.max(winding.nodeA, winding.nodeB));
            elementIndex++;
        }

        netlist = new CircuitNetlist();
        netlist.initNetlist(types,
                nodeX.stream().mapToInt(Integer::intValue).toArray(),
                nodeY.stream().mapToInt(Integer::intValue).toArray(),
                voltageSourceNumbers, params, nodeMax, windings.size(), elementCount);
        netlist.setSingularityEntries(NetlistBuilder.calculateSingularityEntries(
                nodeMax, elementCount,
                nodeX.stream().mapToInt(Integer::intValue).toArray(),
                nodeY.stream().mapToInt(Integer::intValue).toArray()));

        matrixSolver.initializeMatrices(nodeMax, windings.size(), elementCount);

        // Winding element indices and z-rows for the flux readouts; the
        // winding elements start after the linear branches and the two
        // elements per nonlinear branch, carrying source numbers 1..n in
        // registration order
        int windingElementIndex = linearBranches.size() + 2 * nonlinearBranches.size();
        int assignedSourceNumber = 1;
        for (final WindingRuntime winding : windings.values()) {
            winding.elementIndex = windingElementIndex;
            winding.zRow = nodeMax + assignedSourceNumber;
            windingElementIndex++;
            assignedSourceNumber++;
        }
        matricesInitialized = true;
    }

    /**
     * Gets the flux linked by a winding, positive in the direction
     * {@code nodeA -> external magnetic circuit -> nodeB} driven by a positive
     * winding current.
     *
     * @param name winding name
     * @return winding flux of the last step in webers
     *
     * @throws IllegalArgumentException if the winding name is unknown
     */
    public double getWindingFlux(final String name) {
        return requireWinding(name).flux;
    }

    /**
     * Gets the induced EMF of a winding of the last step,
     * {@code v = N * (Phi(t) - Phi(t-dt)) / dt} (Faraday's law of induction).
     *
     * @param name winding name
     * @return induced EMF in volts
     *
     * @throws IllegalArgumentException if the winding name is unknown
     */
    public double getWindingVoltage(final String name) {
        return requireWinding(name).inducedVoltage;
    }

    /**
     * Gets the differential inductance of a winding at the operating point of
     * the last step, {@code L_diff = N * dPhi/di}. The network sensitivity is
     * evaluated by two extra solves of the tangent-permeance (linearized)
     * network with winding MMFs {@code N} and {@code 0} - their difference
     * cancels the Newton companion offsets - ; the converged potentials and
     * the winding MMF are restored afterwards, so the readout has no side
     * effects.
     *
     * <p>For a winding coupled to an electrical inductor,
     * {@code N * dPhi/dt = L_diff * di/dt} - the induced EMF along the
     * saturation curve - so {@code L_diff} is the inductance the electrical
     * domain must present for the coupled dynamics.
     *
     * @param name winding name
     * @return differential inductance in henrys
     *
     * @throws IllegalArgumentException if the winding name is unknown
     * @throws IllegalStateException if no step has been performed yet
     */
    public double getWindingDifferentialInductance(final String name) {
        final WindingRuntime winding = requireWinding(name);
        if (!matricesInitialized) {
            throw new IllegalStateException("Differential inductance requires a completed step");
        }
        final double[] potentials = matrixSolver.getP();
        final double[] saved = potentials.clone();

        // tangent network response to a unit winding current: the nonlinear
        // branch stamps still carry the converged tangent permeances and the
        // Newton companion offsets, so the solve is the exact linearization
        // at the operating point; the offset contribution is identical in
        // both solves and cancels in the difference
        final double[] fluxAtMMF = new double[2];
        for (int solve = 0; solve < 2; solve++) {
            final double mmfFactor = solve;
            netlist.getParameter(winding.elementIndex)[SourceParameters.INDEX_VALUE_DC] =
                    mmfFactor * winding.turns;
            matrixSolver.buildMatrixA(netlist, lastStepWidth, 0.0, false);
            matrixSolver.buildVectorB(netlist, lastStepWidth, 0.0, false);
            matrixSolver.solve();
            fluxAtMMF[solve] = -matrixSolver.getP()[winding.zRow];
        }

        // restore the converged state and the winding MMF
        System.arraycopy(saved, 0, potentials, 0, saved.length);
        netlist.getParameter(winding.elementIndex)[SourceParameters.INDEX_VALUE_DC] =
                winding.turns * winding.current;
        return winding.turns * (fluxAtMMF[1] - fluxAtMMF[0]);
    }

    /**
     * Gets the scalar magnetic potential (MMF) of a node, relative to the
     * reference node of its magnetic island.
     *
     * @param node magnetic node
     * @return node MMF in ampere-turns
     *
     * @throws IllegalArgumentException if the node is null or unknown
     */
    public double getNodeMmf(final MagneticNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Magnetic node must not be null");
        }
        if (node.value() > maxNodeIndex) {
            throw new IllegalArgumentException("Unknown magnetic node " + node);
        }
        if (!matricesInitialized) {
            return 0.0;
        }
        return matrixSolver.getP()[node.value()];
    }

    /**
     * Gets the flux of a reluctance branch (linear or nonlinear), positive in
     * the {@code nodeA -> nodeB} direction of the branch.
     *
     * @param name branch name
     * @return branch flux of the last step in webers
     *
     * @throws IllegalArgumentException if the branch name is unknown
     */
    public double getBranchFlux(final String name) {
        if (!matricesInitialized || !branchNameGuard.containsKey(name)) {
            throw new IllegalArgumentException("Unknown magnetic branch: " + name);
        }
        for (final LinearBranch branch : linearBranches) {
            if (branch.name.equals(name)) {
                return (matrixSolver.getP()[branch.nodeX] - matrixSolver.getP()[branch.nodeY])
                        / branch.reluctance;
            }
        }
        for (final NonlinearBranch branch : nonlinearBranches) {
            if (branch.name.equals(name)) {
                return branch.flux;
            }
        }
        throw new IllegalArgumentException("Unknown magnetic branch: " + name);
    }

    /**
     * Gets the number of registered windings.
     *
     * @return winding count
     */
    public int getWindingCount() {
        return windings.size();
    }

    /**
     * Gets the names of all registered windings in registration order.
     *
     * @return winding names
     */
    public java.util.Set<String> getWindingNames() {
        return java.util.Collections.unmodifiableSet(windings.keySet());
    }

    /**
     * Gets the numerical integration method of the co-simulation.
     *
     * @return solver type
     */
    public SolverType getSolverType() {
        return solverType;
    }

    /**
     * Gets the underlying magnetic netlist, e.g. for result logging of the
     * magnetic channels (node MMFs, branch fluxes).
     *
     * @return the magnetic circuit netlist
     */
    public CircuitNetlist getNetlist() {
        return netlist;
    }

    /**
     * Looks up a winding runtime by name.
     *
     * @param name winding name
     * @return winding runtime
     *
     * @throws IllegalArgumentException if the winding name is unknown
     */
    private WindingRuntime requireWinding(final String name) {
        final WindingRuntime winding = windings.get(name);
        if (winding == null) {
            throw new IllegalArgumentException("Unknown winding: " + name);
        }
        return winding;
    }

    /**
     * Rejects topology changes after the first step.
     *
     * @throws IllegalStateException if the network already stepped
     */
    private void requireModifiable() {
        if (matricesInitialized) {
            throw new IllegalStateException(
                    "Magnetic network topology is fixed after the first step");
        }
    }

    /**
     * Rejects duplicate branch names.
     *
     * @param name candidate branch name
     *
     * @throws IllegalArgumentException if the name is null, blank or already used
     */
    private void requireFreeBranchName(final String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Branch name must not be blank");
        }
        if (branchNameGuard.containsKey(name) || windings.containsKey(name)) {
            throw new IllegalArgumentException("Branch '" + name + "' is already registered");
        }
    }

    /** Linear reluctance branch descriptor. */
    private static final class LinearBranch {
        private final String name;
        private final int nodeX;
        private final int nodeY;
        private final double reluctance;

        LinearBranch(final String name, final int nodeX, final int nodeY, final double reluctance) {
            this.name = name;
            this.nodeX = nodeX;
            this.nodeY = nodeY;
            this.reluctance = reluctance;
        }
    }

    /** Nonlinear reluctance branch with its Newton-Raphson state. */
    private static final class NonlinearBranch {
        private final String name;
        private final int nodeX;
        private final int nodeY;
        private final NonlinearReluctance model;
        private int elementIndex = -1;
        private int offsetElementIndex = -1;
        private double mmfdropIterate;
        private double flux;

        NonlinearBranch(final String name, final int nodeX, final int nodeY,
                        final NonlinearReluctance model) {
            this.name = name;
            this.nodeX = nodeX;
            this.nodeY = nodeY;
            this.model = model;
        }
    }

    /** Per-winding solver runtime state. */
    private static final class WindingRuntime {
        private final int turns;
        private final int nodeA;
        private final int nodeB;
        private double current;
        private int elementIndex = -1;
        private int zRow = -1;
        private double previousFlux;
        private double flux;
        private double inducedVoltage;

        WindingRuntime(final MagneticWinding winding) {
            this.turns = winding.getTurns();
            this.nodeA = winding.getNodeA().value();
            this.nodeB = winding.getNodeB().value();
        }
    }
}
