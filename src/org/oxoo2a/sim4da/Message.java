package org.oxoo2a.sim4da;

import java.lang.reflect.Constructor;

public abstract class Message {

    protected Message() {
    }

    // Copy constructor for base fields
    protected Message(Message original) {
    }

    public Message copy() {
        try {
            Constructor<? extends Message> ctor =
                    this.getClass().getDeclaredConstructor(this.getClass());
            ctor.setAccessible(true);
            return ctor.newInstance(this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Subclass ("+this.getClass()+") must define a proper copy constructor.", e);
        }
    }
}