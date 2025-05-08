package org.oxoo2a.sim4da;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    class Token extends Message {
        private final String token;

        public Token(String token) {
            super();
            this.token = token;
        }

        // Copy constructor for base fields
        private Token(Token original) {
            super(original);
            this.token = original.token;
        }

        @Override
        public Message copy() {
            return new Token(this);
        }
    }

    @BeforeAll
    static void setUp() {
    }

    @Test
    void testMessageBasics() {
        Token token = new Token("token");
        assertEquals("token", token.token);
        assertEquals("Unknown", token.getSender());
    };

}