package org.oxoo2a.sim4da;

/**
 * Primary base class for simulating a node in a distributed algorithm.
 * Sets up networking and provides basic messaging operations.
 */
public class Node {

    /**
     * Constructs a Node with the given name, initializing its network connection.
     *
     * @param name the unique identifier for this node in the network.
     */
    public Node ( String name ) {
        nc = new NetworkConnection(name);
        nc.engage(this::engage);
    }

    /**
     * Called when the network connection engages this node.
     * Default implementation logs a debug message; override to define custom behavior.
     */
    protected void engage () {
        nc.getLogger().debug("Engaging node, but no code defined");
    }

    /**
     * Sends a message to a specific node. If no node is registered under
     * {@code tonodeName}, the send is silently dropped — keeping the
     * algorithm code in {@link #engage} free of try/catch ceremony.
     * Use {@link #sendChecked} when you want unknown recipients to be
     * surfaced as an exception.
     *
     * @param message the Message object to send.
     * @param tonodeName the name of the recipient node.
     */
    protected void send ( Message message, String tonodeName ) {
        nc.send(message, tonodeName);
    }

    /**
     * Sends a message to a specific node, throwing if the recipient is
     * unknown. The strict counterpart to {@link #send}; use this when the
     * algorithm needs to react to a missing recipient (e.g. to retry or to
     * log).
     *
     * @param message the Message object to send.
     * @param tonodeName the name of the recipient node.
     * @throws UnknownNodeException if the destination node is not registered.
     */
    protected void sendChecked ( Message message, String tonodeName ) throws UnknownNodeException {
        nc.sendChecked(message, tonodeName);
    }


    /**
     * Broadcasts a message to all connected nodes.
     *
     * @param message the Message object to broadcast.
     */
    protected void broadcast ( Message message ) {
        nc.send(message);
    }

    /**
     * Receives the next message from the network. Blocks until a message is available.
     *
     * @return the received Message, or null in case of an error (mostly InterruptedException).
     */
    protected ReceivedMessage receive () {
        return nc.receive();
    }

    /**
     * Retrieves this node's unique name.
     *
     * @return the name of this node.
     */
    protected String nodeName () {
        return nc.nodeName();
    }

    /**
     * Pauses execution for the specified duration.
     *
     * @param millis the time to sleep in milliseconds.
     */
    protected void sleep ( int millis ) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    private NetworkConnection nc = null;
}
