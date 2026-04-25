package org.oxoo2a.sim4da.internal;

import org.oxoo2a.sim4da.NetworkConnection;

/**
 * The per-node concentrator for simulation-core capabilities. Every
 * piece of framework state and behavior that a node should be able to
 * observe or invoke — the inbox {@link Mailbox} today, the topology
 * view and the logical clock tomorrow — is held here, on the
 * {@code NodeProxy} belonging to that node.
 *
 * <p>{@link NetworkConnection} is the user-facing handle and surfaces
 * what {@code NodeProxy} carries; {@link org.oxoo2a.sim4da.Node Node}
 * is a thin facade over {@code NetworkConnection}. Adding a new
 * simulation-core feature follows that path: land it on
 * {@code NodeProxy}, expose it on {@code NetworkConnection}, mirror it
 * on {@code Node}.
 */
public class NodeProxy {

    private final Mailbox mailbox = new Mailbox();

    public void deliver(MessageInTransit mit) {
        mailbox.put(mit);
    }

    public MessageInTransit receive() throws InterruptedException {
        return mailbox.take();
    }
}
