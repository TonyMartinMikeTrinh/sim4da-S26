package org.oxoo2a.sim4da.internal;

import org.oxoo2a.sim4da.Message;

/**
 * Represents a message that is currently in transit, including the sender's identifier.
 * This class is used to track messages as they are being sent from one node to another.
 */
public record MessageInTransit(
        Message message,
        String sender
) {
    public MessageInTransit(Message message, String sender) {
        this.message = message.copy();
        this.sender = sender;
    }

    public MessageInTransit(MessageInTransit original) {
        this(original.message, original.sender);
    }
}
