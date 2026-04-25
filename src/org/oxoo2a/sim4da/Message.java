package org.oxoo2a.sim4da;

/**
 * Marker interface for messages exchanged between nodes.
 *
 * <p>Concrete messages should be implemented as records:
 *
 * <pre>{@code
 *     public record Token(int value)    implements Message {}
 *     public record EndMessage()        implements Message {}
 *     public record Probe(String from)  implements Message {}
 * }</pre>
 *
 * <p>Records give messages two properties that matter for a distributed-
 * algorithm simulator:
 *
 * <ul>
 *   <li><b>Immutability</b> — once constructed, a record cannot be modified.
 *       The simulator can therefore share the same instance with every
 *       receiver of a broadcast without defensive copying.</li>
 *   <li><b>Pattern-matching at the receive site</b> — the canonical receive
 *       loop looks like this:
 *       <pre>{@code
 *           switch (receive().message()) {
 *               case Token(int v)   -> sendBlindly(new Token(v + 1), nextId);
 *               case EndMessage e   -> { ...; running = false; }
 *               default             -> throw new IllegalStateException(...);
 *           }
 *       }</pre>
 *       Because record components are deconstructed by the {@code case}
 *       label, the message's data is bound directly into local variables.</li>
 * </ul>
 *
 * <p>Implementations are not required to be records — any class that
 * implements {@code Message} works — but anything that is not effectively
 * immutable risks the usual aliasing hazards once it crosses the network.
 */
public interface Message {}
