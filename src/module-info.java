/**
 * sim4da — a Java framework for simulating distributed algorithms.
 *
 * <p>The module exports exactly one package, {@code org.oxoo2a.sim4da}.
 * The simulation core ({@code Network}, {@code NodeProxy},
 * {@code Mailbox}, {@code MessageInTransit}, the still-empty
 * {@code Topology} and {@code BellTower} stubs) lives in
 * {@code org.oxoo2a.sim4da.internal} and is deliberately <em>not</em>
 * exported. A student's code cannot import or reach those classes —
 * which is precisely the point. The teaching contract is "extend
 * {@link org.oxoo2a.sim4da.Node}, override {@code engage}, use the
 * four verbs"; if patching the simulator core looks easier than that,
 * the framework has failed pedagogically. JPMS makes "patch the
 * simulator core" a compile error rather than a tempting shortcut.
 *
 * <p>{@code requires transitive org.slf4j} re-exports SLF4J so a
 * downstream module that requires {@code org.oxoo2a.sim4da} can
 * declare a variable of type {@link org.slf4j.Logger} (returned by
 * {@link org.oxoo2a.sim4da.NetworkConnection#getLogger()}) without
 * having to add a separate {@code requires org.slf4j;} of its own.
 */
module org.oxoo2a.sim4da {
    requires transitive org.slf4j;

    exports org.oxoo2a.sim4da;
}
