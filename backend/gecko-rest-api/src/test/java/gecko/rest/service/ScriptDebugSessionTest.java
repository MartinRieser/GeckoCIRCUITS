/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.rest.service;

import gecko.core.control.calculators.ScriptDebugContext;
import gecko.core.control.calculators.ScriptDebugSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the blocking semantics of the per-simulation debug session:
 * breakpoint pauses publish a snapshot and block the simulation thread,
 * resume/step commands release it, and a cancellation request unblocks.
 */
class ScriptDebugSessionTest {

    private static final String BLOCK = "CTRL_SCRIPT.1";
    private static final long AWAIT_TIMEOUT_MS = 5000;

    /** Fake execution context simulating one script block's state. */
    private static ScriptDebugContext context(Map<String, Double> variables) {
        return new ScriptDebugContext() {
            @Override
            public String blockName() {
                return BLOCK;
            }

            @Override
            public double timeSeconds() {
                return 0.002;
            }

            @Override
            public double stepWidth() {
                return 1e-6;
            }

            @Override
            public Map<String, Double> userVariables() {
                return variables;
            }

            @Override
            public double[] inputSignals() {
                return new double[]{1.5};
            }

            @Override
            public double[] outputSignals() {
                return new double[]{0.0};
            }
        };
    }

    /** Runs beforeStatement on a worker thread, like the blocked engine thread. */
    private static Thread pauseOnStatementAsync(ScriptDebugSession session,
                                                ScriptDebugContext context, int line) {
        CountDownLatch entered = new CountDownLatch(1);
        Thread worker = new Thread(() -> {
            entered.countDown();
            session.beforeStatement(context, line);
        });
        worker.start();
        try {
            assertTrue(entered.await(AWAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS), "worker did not start");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return worker;
    }

    private static void awaitPaused(ScriptDebugSession session) throws InterruptedException {
        long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
        while (!session.isPaused() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(session.isPaused(), "session did not pause within timeout");
    }

    @Test
    void testBreakpointPausesAndPublishesSnapshot() throws Exception {
        List<ScriptDebugSnapshot> published = new java.util.concurrent.CopyOnWriteArrayList<>();
        ScriptDebugSession session = new ScriptDebugSession(
                Map.of(BLOCK, List.of(3)), published::add, () -> false);

        // Statement on a line without a breakpoint must not pause
        session.beforeStatement(context(Map.of()), 2);
        assertFalse(session.isPaused());
        assertTrue(published.isEmpty());

        // Statement on the breakpoint line pauses the worker thread
        Thread engine = pauseOnStatementAsync(session, context(Map.of("k", 7.5)), 3);
        awaitPaused(session);

        ScriptDebugSnapshot snapshot = session.pausedSnapshot();
        assertEquals(BLOCK, snapshot.blockName());
        assertEquals(3, snapshot.line());
        assertEquals(7.5, snapshot.variables().get("k"), 1e-12);
        assertEquals(0.002, snapshot.time(), 1e-12);
        assertEquals(1, published.size());

        assertTrue(session.release(false));
        engine.join(AWAIT_TIMEOUT_MS);
        assertFalse(engine.isAlive(), "engine thread must be released by resume");
        assertFalse(session.isPaused());
        assertNull(session.pausedSnapshot());
    }

    @Test
    void testStepExecutesOneStatementAndPausesAgain() throws Exception {
        ScriptDebugSession session = new ScriptDebugSession(
                Map.of(BLOCK, List.of(1)), snapshot -> { }, () -> false);

        Thread engine = pauseOnStatementAsync(session, context(Map.of()), 1);
        awaitPaused(session);
        assertTrue(session.release(true));
        engine.join(AWAIT_TIMEOUT_MS);

        // The step is armed for the paused block: its next statement pauses again
        Thread engine2 = pauseOnStatementAsync(session, context(Map.of("k", 1.0)), 4);
        awaitPaused(session);
        assertEquals(4, session.pausedSnapshot().line());

        assertTrue(session.release(false));
        engine2.join(AWAIT_TIMEOUT_MS);

        // After a full resume, further statements run without pausing
        session.beforeStatement(context(Map.of()), 5);
        assertFalse(session.isPaused());
    }

    @Test
    void testCancellationReleasesBlockedHook() {
        ScriptDebugSession session = new ScriptDebugSession(
                Map.of(BLOCK, List.of(5)), snapshot -> { }, () -> true);

        long start = System.currentTimeMillis();
        session.beforeStatement(context(Map.of()), 5);
        long blockedMs = System.currentTimeMillis() - start;

        assertFalse(session.isPaused(), "cancelled run must not stay paused");
        assertTrue(blockedMs < 2000, "cancel check must release the hook quickly");
    }

    @Test
    void testOtherBlocksAreNotPaused() {
        ScriptDebugSession session = new ScriptDebugSession(
                Map.of("OTHER_BLOCK", List.of(1)), snapshot -> { }, () -> false);

        session.beforeStatement(context(Map.of()), 1);
        assertFalse(session.isPaused());
    }

    @Test
    void testSetBreakpointsReplacesAll() throws Exception {
        AtomicReference<ScriptDebugSnapshot> lastSnapshot = new AtomicReference<>();
        ScriptDebugSession session = new ScriptDebugSession(
                Map.of(BLOCK, List.of(1)), lastSnapshot::set, () -> false);

        session.setBreakpoints(Map.of(BLOCK, List.of(2)));

        session.beforeStatement(context(Map.of()), 1);
        assertFalse(session.isPaused(), "old breakpoint must be gone after replacement");

        Thread engine = pauseOnStatementAsync(session, context(Map.of()), 2);
        awaitPaused(session);
        assertEquals(2, session.pausedSnapshot().line());
        session.release(false);
        engine.join(AWAIT_TIMEOUT_MS);
    }
}
