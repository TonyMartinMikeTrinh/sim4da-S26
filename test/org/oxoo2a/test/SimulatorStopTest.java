package org.oxoo2a.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.oxoo2a.sim4da.Node;
import org.oxoo2a.sim4da.ReceivedMessage;
import org.oxoo2a.sim4da.Simulator;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link Simulator#stop()} can be called from inside an
 * Actor's {@code engage()} loop and that the simulation terminates
 * promptly even if some nodes are blocked in {@code receive()} with no
 * incoming messages.
 */
class SimulatorStopTest {

    @Test
    @Timeout(5)
    void stopShortCircuitsAnIndefinitelyBlockedNode() {
        Simulator simulator = Simulator.getInstance();

        AtomicReference<ReceivedMessage> receivedAfterStop = new AtomicReference<>();
        AtomicBoolean listenerReturned = new AtomicBoolean(false);

        // No messages will ever be sent to this node — receive() blocks
        // forever unless the simulation is stopped.
        new Node("listener") {
            @Override
            protected void engage() {
                receivedAfterStop.set(receive());
                listenerReturned.set(true);
            }
        };

        // Triggers stop() shortly after the simulation begins.
        new Node("trigger") {
            @Override
            protected void engage() {
                sleep(100);
                Simulator.getInstance().stop();
            }
        };

        simulator.simulate();
        simulator.shutdown();

        assertTrue(listenerReturned.get(),
                "listener's engage() must return once stop() is called");
        assertNull(receivedAfterStop.get(),
                "receive() must return null after stop()");
    }
}
