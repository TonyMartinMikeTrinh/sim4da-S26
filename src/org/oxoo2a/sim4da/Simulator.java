package org.oxoo2a.sim4da;

import org.oxoo2a.sim4da.internal.Network;
import org.oxoo2a.sim4da.internal.sim4da;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Lifecycle controller for a sim4da run. There is exactly one Simulator
 * per program — students don't need more than that, and the singleton
 * makes the constraint explicit.
 *
 * <p>Typical lifecycle:
 *
 * <pre>{@code
 *     Simulator simulator = Simulator.getInstance();
 *     // ... create your nodes ...
 *     simulator.simulate();          // or simulate(long durationInSeconds)
 *     simulator.shutdown();          // resets framework state for the next run
 * }</pre>
 *
 * <p>Any node can call {@link #stop()} from inside {@code engage()} to end
 * the simulation early — useful for algorithm-driven termination
 * (leader-election success, snapshot complete, Dijkstra-Scholten quiescence).
 * {@link #shutdown()} resets all framework state, so a JUnit suite that
 * runs many tests in the same JVM remains independent.
 */
public class Simulator {
    private final String version = "sim4da Summer 2025";
    private  Simulator () {
        System.setProperty("PID", String.valueOf(ProcessHandle.current().pid())); // Needed for logback
        logger = LoggerFactory.getLogger(sim4da.class);
        System.out.println(version);
        logger.info(version + " - Simulation started.");
    }

    public static Simulator getInstance() {
        if (instance == null) {
            synchronized (Simulator.class) {
                if (instance == null) {
                    instance = new Simulator();
                }
            }
        }
        return instance;
    }

    /**
     * Runs the simulation for at most {@code durationInSeconds} seconds, or
     * until {@link #stop()} is called from anywhere — whichever comes first.
     * Returns once all node threads have terminated.
     */
    public void simulate ( long durationInSeconds ) {
        simulating = true;
        startSignal.countDown();
        try {
            // Block until the duration elapses or stop() fires the latch.
            //noinspection ResultOfMethodCallIgnored
            stopSignal.await(durationInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        simulating = false;
        List<NetworkConnection> ncs = Network.getInstance().getAllNetworkConnections();
        for (NetworkConnection nc : ncs) {
            nc.interrupt();
        }
        for (NetworkConnection nc : ncs ) {
            nc.join();
        }
    }

    /**
     * Runs the simulation until every node terminates on its own — either
     * by returning from {@code engage()} naturally, or because some node
     * called {@link #stop()}.
     */
    public void simulate () {
        simulating = true;
        startSignal.countDown();
        List<NetworkConnection> ncs = Network.getInstance().getAllNetworkConnections();
        for (NetworkConnection nc : ncs) {
            nc.join();
        }
    }

    /**
     * Requests an immediate end to the simulation. Safe to call from any
     * thread — including from inside an Actor's {@code engage()} loop.
     * Every node's {@link Node#receive() receive} returns {@code null}, the
     * timeout in {@link #simulate(long)} is short-circuited, and the
     * matching {@code simulate*} method returns once all nodes have exited.
     */
    public void stop() {
        simulating = false;
        stopSignal.countDown();
        for (NetworkConnection nc : Network.getInstance().getAllNetworkConnections()) {
            nc.interrupt();
        }
    }

    /**
     * Resets the framework so a fresh {@code simulate} call can run. Clears
     * the node registry, replaces the start/stop latches, and clears the
     * configurable simulation behavior. Call this at the end of every
     * simulation — once per test method in a JUnit suite.
     */
    public void shutdown () {
        Network.getInstance().shutdown();
        SimulationBehavior.reset();
        startSignal = new CountDownLatch(1);
        stopSignal = new CountDownLatch(1);
        simulating = false;
        logger.info(version + " - Simulation ended.");
    }

    public boolean isSimulating() {
        return simulating;
    }
    private static Simulator instance = null;
    private final Logger logger;
    private volatile boolean simulating = false;
    private CountDownLatch startSignal = new CountDownLatch(1);
    private CountDownLatch stopSignal = new CountDownLatch(1);

    public void awaitSimulationStart() {
        if (simulating) return;
        try {
            startSignal.await();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
