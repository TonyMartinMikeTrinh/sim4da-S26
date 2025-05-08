package org.oxoo2a.sim4da;

public abstract class Message {

    private String sender;

    protected Message() {
        this.sender = "Unknown";
    }

    // Copy constructor for base fields
    protected Message(Message original) {
        this.sender = original.sender;
    }

    protected void setSender(String sender) {
        this.sender = sender;
    }

    public String getSender() {
        return sender;
    }

    // enforce copy constructor implementation in subclasses
    public abstract Message copy();
}