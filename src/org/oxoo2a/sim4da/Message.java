package org.oxoo2a.sim4da;

import java.lang.reflect.Constructor;

public abstract class Message {

    protected Message() {
    }

    // Copy constructor for base fields
    protected Message(Message original) {
    }

    /**
     * Creates a copy of this message.
     * This method uses reflection to invoke the copy constructor of the subclass.
     * Subclasses must define a proper copy constructor that takes an instance of the same class.
     *
     * @return a new instance of the same class with copied fields.
     * @throws RuntimeException if the subclass does not define a proper copy constructor.
     */
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