package org.oxoo2a.sim4da.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * The framework's event log.
 *
 * <p>Every send, every receive, and every algorithm-level event a student
 * chooses to record via {@link org.oxoo2a.sim4da.NetworkConnection#log}
 * appears here as a single line:
 *
 * <pre>{@code
 *   <ISO 8601 timestamp> [<source>] <event message>
 * }</pre>
 *
 * <p>Per-node chronological order is preserved automatically because each
 * node calls {@link #record} from its own virtual thread, and the calls
 * within one thread are serial. Cross-node order is whatever the JVM's
 * scheduler produces — for an event-driven simulator that <em>is</em> the
 * observed timeline. Students analyse the merged log offline to
 * investigate liveness, safety, and ordering properties of their
 * algorithm.
 *
 * <p>Output goes to {@code sim4da-<PID>.log} in the working directory.
 * The implementation deliberately does not pull in SLF4J / Logback or
 * any other logging framework — sim4da's needs are simple enough that
 * adding one would be using a sledgehammer to crack a nut, and a
 * dependency-free framework distributes as a single, standalone JAR.
 */
public final class EventLog {

    private static EventLog instance;

    public static synchronized EventLog getInstance() {
        if (instance == null) instance = new EventLog();
        return instance;
    }

    private final PrintWriter writer;

    private EventLog() {
        long pid = ProcessHandle.current().pid();
        Path file = Path.of("sim4da-" + pid + ".log");
        try {
            this.writer = new PrintWriter(
                    Files.newBufferedWriter(file,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND),
                    true);    // autoFlush
        } catch (IOException e) {
            throw new RuntimeException("Cannot open log file " + file, e);
        }
    }

    /**
     * Append one event to the log. {@link PrintWriter#println(String)}
     * is synchronized internally, so concurrent callers each produce a
     * complete line — never interleaved characters.
     */
    public void record(String source, String event) {
        writer.println(Instant.now() + " [" + source + "] " + event);
    }
}
