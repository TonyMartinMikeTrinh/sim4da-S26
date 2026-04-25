package org.oxoo2a.sim4da.internal;

import org.oxoo2a.sim4da.NetworkConnection;

/**
 * The framework-side handle representing the simulated distributed
 * infrastructure as the Actor (the user's algorithm) sees it.
 *
 * <p>The Actor interacts with the network via its {@link NetworkConnection};
 * on the other side of the wire, this {@code NodeProxy} is what the
 * {@link Network} delivers to. Today it carries one component — the
 * {@link Mailbox} — and is the natural home for per-node infrastructure
 * yet to come (topology view, logical clock, scheduling hooks).
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
