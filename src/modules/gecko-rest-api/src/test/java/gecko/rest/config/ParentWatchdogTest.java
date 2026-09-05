package gecko.rest.config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the orphaned-engine shutdown logic with fake liveness predicates,
 * no real processes involved.
 */
class ParentWatchdogTest {

    @Test
    void triggersExitWhenParentIsGone() throws Exception {
        CountDownLatch exited = new CountDownLatch(1);
        ParentWatchdog watchdog = new ParentWatchdog(4242, 10, pid -> false, exited::countDown);

        watchdog.start();
        assertTrue(exited.await(5, TimeUnit.SECONDS), "watchdog should fire when parent is dead");
        assertFalse(watchdog.isRunning());
    }

    @Test
    void keepsRunningWhileParentIsAlive() throws Exception {
        CountDownLatch exited = new CountDownLatch(1);
        ParentWatchdog watchdog = new ParentWatchdog(4242, 20, pid -> true, exited::countDown);

        watchdog.start();
        assertFalse(exited.await(150, TimeUnit.MILLISECONDS), "watchdog must not fire while parent lives");
        assertTrue(watchdog.isRunning());
        watchdog.stop();
        assertFalse(watchdog.isRunning());
    }

    @Test
    void doesNothingWithoutParentPid() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ParentWatchdog watchdog = new ParentWatchdog(0, 10, pid -> false, calls::incrementAndGet);

        watchdog.start();
        Thread.sleep(60);
        assertEquals(0, calls.get(), "watchdog must stay disabled when parent pid is 0");
        assertFalse(watchdog.isRunning());
    }

    @Test
    void stopPreventsTheExitStrategy() throws Exception {
        AtomicBoolean fired = new AtomicBoolean(false);
        // predicate flips to dead only after stop() — a stopped watchdog must not fire
        AtomicBoolean alive = new AtomicBoolean(true);
        ParentWatchdog watchdog = new ParentWatchdog(4242, 10, pid -> alive.get(), () -> fired.set(true));

        watchdog.start();
        watchdog.stop();
        alive.set(false);
        Thread.sleep(60);
        assertFalse(fired.get(), "stopped watchdog must never trigger the exit strategy");
    }
}
