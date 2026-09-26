/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.rest.service;

import gecko.core.control.calculators.ScriptDebugContext;
import gecko.core.control.calculators.ScriptDebugHook;
import gecko.core.control.calculators.ScriptDebugSnapshot;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Per-simulation debug session realizing script breakpoints and single
 * stepping. Implements the engine's {@link ScriptDebugHook}: when a statement's
 * source line carries a breakpoint (or a step is armed), the session publishes
 * a snapshot and blocks the simulation thread until a resume/step command
 * arrives from the REST layer or the run is cancelled.
 *
 * <p>Breakpoint updates and resume commands arrive on Tomcat request threads
 * while the engine thread is blocked inside {@link #beforeStatement}, so all
 * shared state is concurrent or volatile.
 */
public final class ScriptDebugSession implements ScriptDebugHook {

    /** Resume commands offered to the blocked simulation thread. */
    private enum ResumeMode { RESUME, STEP }

    /** Poll interval while blocked, so cancellation requests are noticed. */
    private static final long POLL_INTERVAL_MS = 100;

    /** Grace period for a resume command to be picked up after offering. */
    private static final long RESUME_OFFER_TIMEOUT_MS = 2000;

    private final Map<String, Set<Integer>> breakpoints = new ConcurrentHashMap<>();
    private final SynchronousQueue<ResumeMode> resumeCommands = new SynchronousQueue<>();
    private final Consumer<ScriptDebugSnapshot> pauseListener;
    private final BooleanSupplier cancelCheck;

    private volatile boolean stepArmed;
    private volatile String steppingBlock = "";
    private volatile ScriptDebugSnapshot pausedSnapshot;

    /** Start (nanoTime) of the current pause, 0 when not paused. */
    private volatile long pauseStartNanos;
    /** Accumulated pause wall time (nanos) across the whole run. */
    private final AtomicLong totalPausedNanos = new AtomicLong();

    /**
     * Creates a session.
     *
     * @param initialBreakpoints block name to breakpoint source lines, may be null/empty
     * @param pauseListener notified with the snapshot whenever a breakpoint pauses the run
     * @param cancelCheck supplies true when the simulation was cancelled, releasing a blocked hook
     */
    public ScriptDebugSession(Map<String, List<Integer>> initialBreakpoints,
                              Consumer<ScriptDebugSnapshot> pauseListener,
                              BooleanSupplier cancelCheck) {
        this.pauseListener = pauseListener;
        this.cancelCheck = cancelCheck;
        if (initialBreakpoints != null) {
            setBreakpoints(initialBreakpoints);
        }
    }

    /**
     * Replaces all breakpoints of the session (block name to source lines).
     * Takes effect at the next statement, including while paused.
     *
     * @param linesByBlock block name to 1-based source lines
     */
    public void setBreakpoints(Map<String, List<Integer>> linesByBlock) {
        breakpoints.clear();
        if (linesByBlock == null) {
            return;
        }
        for (Map.Entry<String, List<Integer>> entry : linesByBlock.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                breakpoints.put(entry.getKey(), Set.copyOf(entry.getValue()));
            }
        }
    }

    /** True while the simulation thread is blocked at a breakpoint. */
    public boolean isPaused() {
        return pausedSnapshot != null;
    }

    /**
     * Snapshot of the pause state, or null when not paused.
     *
     * @return the snapshot captured when the breakpoint hit
     */
    public ScriptDebugSnapshot pausedSnapshot() {
        return pausedSnapshot;
    }

    /**
     * Releases a blocked simulation thread.
     *
     * @param step true to execute exactly one more statement of the paused
     *        block and pause again, false to run until the next breakpoint
     * @return true when the command was delivered to the paused engine thread
     */
    public boolean release(boolean step) {
        try {
            return resumeCommands.offer(step ? ResumeMode.STEP : ResumeMode.RESUME,
                    RESUME_OFFER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void beforeStatement(ScriptDebugContext context, int line) {
        if (!shouldPause(context.blockName(), line)) {
            return;
        }
        pauseAndBlock(context, line);
    }

    private boolean shouldPause(String blockName, int line) {
        if (stepArmed && blockName.equals(steppingBlock)) {
            return true;
        }
        Set<Integer> lines = breakpoints.get(blockName);
        return lines != null && lines.contains(line);
    }

    /**
     * Publishes the pause snapshot and blocks the simulation thread until a
     * resume/step command arrives or the run is cancelled.
     */
    private void pauseAndBlock(ScriptDebugContext context, int line) {
        ScriptDebugSnapshot snapshot = new ScriptDebugSnapshot(context.blockName(), line,
                context.timeSeconds(), context.stepWidth(),
                Map.copyOf(context.userVariables()),
                context.inputSignals().clone(), context.outputSignals().clone());
        pausedSnapshot = snapshot;
        pauseStartNanos = System.nanoTime();
        try {
            if (pauseListener != null) {
                pauseListener.accept(snapshot);
            }
            while (true) {
                ResumeMode mode = resumeCommands.poll(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
                if (mode != null) {
                    stepArmed = mode == ResumeMode.STEP;
                    steppingBlock = stepArmed ? context.blockName() : "";
                    return;
                }
                if (cancelCheck.getAsBoolean()) {
                    clearStepState();
                    return;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            clearStepState();
        } finally {
            totalPausedNanos.addAndGet(System.nanoTime() - pauseStartNanos);
            pauseStartNanos = 0;
            pausedSnapshot = null;
        }
    }

    private void clearStepState() {
        stepArmed = false;
        steppingBlock = "";
    }

    /**
     * Wall-clock time in milliseconds the simulation thread has spent blocked
     * at script breakpoints. Budget checks add it to the deadline so debug
     * sessions do not consume the run's time budget.
     *
     * @return accumulated breakpoint pause wall time in milliseconds
     */
    public long getTotalPausedTimeMs() {
        return totalPausedNanos.get() / 1_000_000;
    }
}
