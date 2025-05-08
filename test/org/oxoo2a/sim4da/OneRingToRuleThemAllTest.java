package org.oxoo2a.sim4da;

import org.junit.jupiter.api.Test;

public class OneRingToRuleThemAllTest {

    class Token extends Message {
        public String token;
        public int value;

        public Token(String token) {
            super();
            this.token = token;
            this.value = 0;
        }

        // Copy constructor for base fields
        private Token(Token original) {
            super(original);
            this.token = original.token;
            this.value = original.value;
        }

        @Override
        public Message copy() {
            return new Token(this);
        }
    }

    class Coordinator {
        private final int waitTime;

        public Coordinator( int waitTime) {
            this.waitTime = waitTime;
        }

        public void engage () {
            nc.engage(this::start);
        }

        private void start() {
            Token t = new Token("token");
            nc.sendBlindly(t, "0");
            try {
                Thread.sleep(waitTime);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            t = new Token("end");
            nc.sendBlindly(t, "0");
        }

        private final NetworkConnection nc = new NetworkConnection("Coordinator");
    }

    class RingSegment extends Node {
        public RingSegment(int id, int next_id) {
            super(String.valueOf(id));
            this.id = String.valueOf(id);
            this.next_id = String.valueOf(next_id);
        }

        @Override
        public void engage() {
            boolean running = true;
            while (running) {
                Message received = receive();
                switch (received) {
                    case Token t -> {
                        System.out.printf("Ring segment %s received token %s from %s%n", NodeName(), t.token, t.getSender());
                        String value = t.token;
                        if (value.equals("end")) {
                            System.out.printf("Ring segment %s received end message; terminating.\n", NodeName());
                            sendBlindly(t, next_id);
                            running = false;
                        } else {
                            // sleep(500);
                            int v = t.value;
                            System.out.printf("Ring segment %s received token %d\n", NodeName(), v);
                            t.value++;
                            sendBlindly(t, next_id);
                        }
                    }
                    default -> {
                        throw new IllegalStateException("Unexpected message: " + received.getClass());
                    }
                }
            }
        }

        private final String id;
        private final String next_id;
    }

    @Test
    void testOneRingToRuleThemAll() {
        final int ringSize = 20;
        Simulator simulator = Simulator.getInstance();
        RingSegment[] segments = new RingSegment[ringSize];
        for (int i = 0; i < ringSize; i++) {
            segments[i] = new RingSegment(i, (i+1) % ringSize);
        }
        Coordinator coordinator = new Coordinator(5000);
        coordinator.engage();

        simulator.simulate();
        simulator.shutdown();
    }
}
