package gecko.rest.config;

import java.util.function.LongPredicate;

/**
 * Shuts the engine down when the desktop shell (its parent process) dies,
 * so a crashed shell never leaves an orphaned Java server bound to a local
 * port. Disabled when {@code gecko.parent-pid} is 0 (default), which is the
 * case for browser/dev deployments.
 */
public class ParentWatchdog {

    private final long parentPid;
    private final long intervalMs;
    private final LongPredicate parentAlive;
    private final Runnable onParentGone;
    private volatile boolean running;
    private Thread thread;

    public ParentWatchdog(long parentPid, long intervalMs, LongPredicate parentAlive, Runnable onParentGone) {
        this.parentPid = parentPid;
        this.intervalMs = intervalMs;
        this.parentAlive = parentAlive;
        this.onParentGone = onParentGone;
    }

    /** Starts the background check; no-op when no parent pid is configured. */
    public synchronized void start() {
        if (running || parentPid <= 0) {
            return;
        }
        running = true;
        thread = new Thread(this::watchLoop, "gecko-parent-watchdog");
        thread.setDaemon(true);
        thread.start();
    }

    /** Stops the background check without triggering the exit strategy. */
    public synchronized void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    boolean isRunning() {
        return running;
    }

    private void watchLoop() {
        while (running) {
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!parentAlive.test(parentPid)) {
                running = false;
                onParentGone.run();
                return;
            }
        }
    }
}
