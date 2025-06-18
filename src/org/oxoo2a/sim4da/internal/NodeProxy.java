package org.oxoo2a.sim4da.internal;

import org.oxoo2a.sim4da.Message;
import org.oxoo2a.sim4da.internal.MessageInTransit;
import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.SimulationBehavior;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NodeProxy {
    public NodeProxy ( NetworkConnection nc ) {
        this.nc = nc;
    }

    public void deliver (MessageInTransit mit, NetworkConnection sender ) {
        synchronized (messages) {
            messages.add(new ReceivedMessage(mit, sender));
            messages.notify();
        }
    }

    public MessageInTransit receive () {
        synchronized (messages) {
            while (messages.isEmpty()) {
                try {
                    messages.wait();
                } catch (InterruptedException e) {
                    // TODO Signal associated NetworkConnection that the simulation is shutting down
                    e.printStackTrace();
                }
            }
            int candidate_index = SimulationBehavior.selectMessageInQueue(messages.size());
            ReceivedMessage candidate = messages.remove(candidate_index);
            return candidate.mit;
        }
    }
    private record ReceivedMessage ( MessageInTransit mit, NetworkConnection sender ) {};

    private final List<ReceivedMessage> messages = Collections.synchronizedList(new ArrayList<>());
    private final NetworkConnection nc;
}
