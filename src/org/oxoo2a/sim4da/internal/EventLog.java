package org.oxoo2a.sim4da.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * The framework's event log.
 *
 * <p>Every send, every receive, and every algorithm-level event a student
 * chooses to record via {@link org.oxoo2a.sim4da.NetworkConnection#log}
 * appears here as a single line:
 *
 * <pre>{@code
 *   [<source>,<seq>] <event message>
 * }</pre>
 *
 * <p>The {@code seq} is a monotonically increasing per-source counter —
 * for a node, it is the number of events that node has produced so far.
 * Reading a single source's events in increasing {@code seq} order
 * gives that source's local timeline; cross-source order is left
 * unspecified (the file's commit order is an artifact of the
 * simulator's host clock, not a meaningful global time). This deliberate
 * absence of a wall-clock timestamp pushes students to reconstruct
 * causality from the messages and local sequence numbers — the way one
 * has to reason in a real distributed system.
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
     * Append one event to the log. The caller supplies its own per-source
     * sequence number. {@link PrintWriter#println(String)} is synchronized
     * internally, so concurrent callers each produce a complete line —
     * never interleaved characters.
     */
    public void record(String source, long seq, String event) {
        writer.println("[" + source + "," + seq + "] " + event);
    }
}
