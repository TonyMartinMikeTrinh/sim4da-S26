package org.oxoo2a.uebung2;

import org.junit.jupiter.api.Test;
import org.oxoo2a.sim4da.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Übungsblatt 2 — Schnappschuss eines konsistenten globalen Zustands.
 *
 * <p>Aufgabe 1: Das "Bankhaus" — n vollständig vernetzte Prozesse mit je einem Konto
 * (Startsaldo 1000), die einander wiederholt zufällige Beträge überweisen. Invariante:
 * Σ Konten + Σ unterwegs befindliche Überweisungen = S (konstant).
 *
 * <p>Aufgabe 2: Variante (a), Koordinator-/Einfärbeverfahren. Die Farbe ist als
 * Epochennummer umgesetzt (Epoche &lt; Runde r = "weiß" bzgl. r, sonst "schwarz"),
 * damit mehrere Schnappschüsse nacheinander laufen können. Jede Basisnachricht trägt
 * die Epoche ihres Senders zum Sendezeitpunkt. Das Verfahren ist bewusst statt
 * Chandy-Lamport gewählt: Die FIFO-Zustellung ist abgeschaltet (siehe Aufgabenblatt),
 * und Marker funktionieren ohne FIFO nicht — die Einfärbung reist dagegen mit jeder
 * Nachricht selbst und ist gegen beliebige Umordnung immun.
 *
 * <p>Aufgabe 3: Naiver Vergleichsschnappschuss (nur Kontostände, Kanäle ignoriert)
 * sowie Statistik über n und Überweisungsfrequenz — siehe die drei Tests unten.
 */
public class BankhausTest {

    // ---------------------------------------------------------------- Nachrichten

    /** Selbstnachricht: die nächste eigene Überweisung ist fällig. */
    record Tick() implements Message {}

    /** Basisnachricht: Überweisung, eingefärbt mit der Epoche des Senders zum Sendezeitpunkt. */
    record Transfer(int amount, int epoch) implements Message {}

    /** Kontrollnachricht des Koordinators: "state?" mit neuer Farbe (Rundennummer). */
    record StateRequest(int round) implements Message {}

    /** Zustandsmeldung eines Prozesses: Saldo plus Zähler für die Terminierungserkennung. */
    record StateReport(int round, int balance, int sent, int received) implements Message {}

    /** Weiße Nachricht, die bei einem schwarzen Prozess eintraf: Kanalzustand des Schnitts. */
    record ChannelMessage(int amount, String from, String to) implements Message {}

    /** Naiver Schnappschuss (Aufgabe 3.2): fragt nur den Kontostand ab. */
    record NaiveRequest() implements Message {}
    record NaiveReport(int balance) implements Message {}

    // ---------------------------------------------------------------- Szenario

    /** Parameter eines Simulationslaufs: n Banken + 1 Koordinator, Takt- und Latenzfenster. */
    record Scenario(String prefix, int n, int rounds, int settleMs,
                    int tickMinMs, int tickMaxMs, int latencyMinMs, int latencyMaxMs) {
        static final int START_BALANCE = 1000;
        private static final AtomicInteger RUNS = new AtomicInteger();

        /** Eindeutiges Namenspräfix pro Lauf: verspätete Zustellungen aus einem
         *  früheren Lauf finden keinen gleichnamigen Empfänger und verpuffen. */
        static Scenario of(int n, int rounds, int settleMs,
                           int tickMinMs, int tickMaxMs, int latencyMinMs, int latencyMaxMs) {
            return new Scenario("B" + RUNS.incrementAndGet(), n, rounds, settleMs,
                    tickMinMs, tickMaxMs, latencyMinMs, latencyMaxMs);
        }

        int totalMoney() { return n * START_BALANCE; }
        String nodeName(int id) { return prefix + "-" + id; }
        String coordinatorName() { return prefix + "-Koordinator"; }

        String randomOtherNode(int selfId) {
            int other = ThreadLocalRandom.current().nextInt(n - 1);
            return nodeName(other >= selfId ? other + 1 : other);
        }
    }

    // ---------------------------------------------------------------- Aufgabe 1

    /** Ein Prozess des Bankhauses: führt ein Konto und überweist zufällige Beträge. */
    static class BankNode extends Node {
        private final Scenario sc;
        private final int id;
        private int balance = Scenario.START_BALANCE;
        private int epoch = 0;    // "Farbe": epoch < r heißt weiß bzgl. Runde r, sonst schwarz
        private int sent = 0;     // kumulativ gesendete Transfers (für die Terminierungserkennung)
        private int received = 0; // kumulativ verbuchte Transfers

        BankNode(Scenario sc, int id) {
            super(sc.nodeName(id));
            this.sc = sc;
            this.id = id;
        }

        @Override
        protected void engage() {
            sendLater(new Tick(), nodeName(), sc.tickMinMs(), sc.tickMaxMs());
            while (true) {
                ReceivedMessage rm = receive();
                if (rm == null) return; // Simulationsende
                switch (rm.message()) {
                    case Tick() -> {
                        transfer();
                        sendLater(new Tick(), nodeName(), sc.tickMinMs(), sc.tickMaxMs());
                    }
                    case Transfer(int amount, int msgEpoch) -> {
                        if (msgEpoch > epoch) {
                            // Schwarze Nachricht bei weißem Prozess ("Nachricht aus der Zukunft"):
                            // erst den eigenen Zustand sichern, dann verbuchen. So rückt der
                            // Empfang hinter den Schnitt und der Schnitt bleibt konsistent.
                            takeSnapshot(msgEpoch);
                        } else if (msgEpoch < epoch) {
                            // Weiße Nachricht bei schwarzem Prozess: war zum Schnittzeitpunkt
                            // im Kanal → gehört zum Kanalzustand, Kopie an den Koordinator.
                            send(new ChannelMessage(amount, rm.sender(), nodeName()), sc.coordinatorName());
                        }
                        balance += amount;
                        received++;
                    }
                    case StateRequest(int round) -> {
                        if (round > epoch) takeSnapshot(round); // sonst: schon via Basisnachricht schwarz
                    }
                    case NaiveRequest() -> send(new NaiveReport(balance), sc.coordinatorName());
                    default -> throw new IllegalStateException("Unerwartete Nachricht: " + rm.message());
                }
            }
        }

        /** Weiß → schwarz: lokalen Zustand festhalten und an den Koordinator melden. */
        private void takeSnapshot(int round) {
            epoch = round;
            send(new StateReport(round, balance, sent, received), sc.coordinatorName());
        }

        /** Überweist einen zufälligen Betrag 0 &lt; b ≤ balance an einen zufälligen anderen Prozess. */
        private void transfer() {
            if (balance == 0) return;
            int amount = ThreadLocalRandom.current().nextInt(1, balance + 1);
            balance -= amount; // sofort abbuchen — ab jetzt ist der Betrag "unterwegs"
            sent++;
            sendLater(new Transfer(amount, epoch), sc.randomOtherNode(id), sc.latencyMinMs(), sc.latencyMaxMs());
        }

        /**
         * Zustellung mit zufälliger Verzögerung: modelliert die im Aufgabenblatt geforderte
         * Übertragungslatenz — ohne sie wären die Kanäle praktisch immer leer. Auch der
         * eigene Überweisungstakt (Tick) nutzt diesen Mechanismus.
         */
        private void sendLater(Message m, String to, int minMs, int maxMs) {
            int delay = ThreadLocalRandom.current().nextInt(minMs, maxMs + 1);
            Thread.startVirtualThread(() -> {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    return; // Simulationsende
                }
                send(m, to);
            });
        }
    }

    // ---------------------------------------------------------------- Aufgabe 2 + 3

    /** Löst abwechselnd eingefärbte und naive Schnappschüsse aus und sammelt die Ergebnisse. */
    static class Coordinator extends Node {
        private final Scenario sc;
        final List<Integer> consistentTotals = new ArrayList<>(); // Σ Konten + Kanäle je Schnappschuss
        final List<Integer> naiveTotals = new ArrayList<>();      // Σ Konten des naiven Verfahrens
        final List<Integer> controlMessages = new ArrayList<>();  // Kontrollnachrichten je Schnappschuss
        final List<Integer> channelMessages = new ArrayList<>();  // erfasste Kanalnachrichten je Schnappschuss
        volatile boolean done = false;

        Coordinator(Scenario sc) {
            super(sc.coordinatorName());
            this.sc = sc;
        }

        @Override
        protected void engage() {
            for (int round = 1; round <= sc.rounds(); round++) {
                sleep(sc.settleMs()); // Betrieb weiterlaufen lassen — Schnappschuss mitten im Geschehen
                if (!coloredSnapshot(round) || !naiveSnapshot()) return; // Simulationsende
            }
            done = true;
        }

        /** Einfärbeverfahren: "state?" per Multicast, dann Zustände und Kanalnachrichten einsammeln. */
        private boolean coloredSnapshot(int round) {
            broadcast(new StateRequest(round));
            Map<String, Integer> balances = new TreeMap<>();
            List<ChannelMessage> channel = new ArrayList<>();
            int sentTotal = 0, receivedTotal = 0;
            while (true) {
                ReceivedMessage rm = receive();
                if (rm == null) return false;
                switch (rm.message()) {
                    case StateReport(int r, int balance, int snt, int rcvd) -> {
                        if (r != round) throw new IllegalStateException("Meldung aus fremder Runde: " + r);
                        balances.put(rm.sender(), balance);
                        sentTotal += snt;
                        receivedTotal += rcvd;
                    }
                    case ChannelMessage cm -> channel.add(cm);
                    default -> throw new IllegalStateException("Unerwartete Nachricht: " + rm.message());
                }
                // Terminierung durch Zählen (FIFO-unabhängig): Zum Schnittzeitpunkt waren genau
                // Σsent − Σreceived Nachrichten unterwegs; jede davon trifft irgendwann bei einem
                // schwarzen Prozess ein und wird als genau eine ChannelMessage nachgemeldet.
                if (balances.size() == sc.n() && channel.size() == sentTotal - receivedTotal) break;
            }
            int balanceSum = balances.values().stream().mapToInt(Integer::intValue).sum();
            int channelSum = channel.stream().mapToInt(ChannelMessage::amount).sum();
            String channels = channel.isEmpty() ? "—" : channel.stream()
                    .map(c -> c.from() + "→" + c.to() + ": " + c.amount())
                    .collect(Collectors.joining(", "));
            System.out.printf("[Schnappschuss %d] Konten %s  Σ=%d%n", round, balances, balanceSum);
            System.out.printf("[Schnappschuss %d] Kanäle: %s  Σ=%d%n", round, channels, channelSum);
            System.out.printf("[Schnappschuss %d] Gesamtsumme %d, S=%d%n", round, balanceSum + channelSum, sc.totalMoney());
            consistentTotals.add(balanceSum + channelSum);
            channelMessages.add(channel.size());
            controlMessages.add(2 * sc.n() + channel.size()); // n × state? + n Meldungen + Kanalnachrichten
            return true;
        }

        /** Naiver Schnappschuss (Aufgabe 3.2): nur Kontostände, Kanäle werden ignoriert. */
        private boolean naiveSnapshot() {
            broadcast(new NaiveRequest());
            int sum = 0;
            for (int i = 0; i < sc.n(); i++) {
                ReceivedMessage rm = receive();
                if (rm == null) return false;
                sum += ((NaiveReport) rm.message()).balance();
            }
            System.out.printf("[Naiv]           Σ Konten=%d, S=%d%s%n", sum, sc.totalMoney(),
                    sum == sc.totalMoney() ? "" : "  ← inkonsistent, Differenz " + (sum - sc.totalMoney()));
            naiveTotals.add(sum);
            return true;
        }
    }

    // ---------------------------------------------------------------- Simulationslauf

    /** Baut das Bankhaus auf, lässt es laufen und liefert den Koordinator samt Ergebnissen. */
    private static Coordinator run(Scenario sc) {
        Simulator simulator = Simulator.getInstance();
        simulator.disableLogging(); // Hunderte Transfers pro Sekunde — Logdatei bringt hier nichts
        // FIFO-Zustellung abschalten (siehe Aufgabenblatt): Empfänger ziehen eine
        // zufällige Nachricht aus ihrer Warteschlange, Umordnung ist damit die Regel.
        SimulationBehavior.setMessageQueueSelectionDistributionFunction(RandomValues.getUniformDistribution());
        try {
            for (int i = 0; i < sc.n(); i++) new BankNode(sc, i);
            Coordinator coordinator = new Coordinator(sc);
            // Beendet die Simulation von außen (nie aus einem Knoten!), sobald der Koordinator fertig ist.
            Thread watcher = new Thread(() -> {
                while (!coordinator.done) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                simulator.stop();
            });
            watcher.start();
            simulator.simulate(60); // Obergrenze — endet über den Watcher normalerweise deutlich früher
            watcher.interrupt();
            try {
                watcher.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return coordinator;
        } finally {
            simulator.shutdown();
        }
    }

    // ---------------------------------------------------------------- Tests

    /** Aufgaben 1+2+3.1: Jeder eingefärbte Schnappschuss liefert Konten + Kanäle = S. */
    @Test
    void aufgabe1und2_eingefaerbterSchnappschussIstKonsistent() {
        Scenario sc = Scenario.of(5, 4, 400, 20, 100, 50, 250);
        Coordinator c = run(sc);
        assertEquals(sc.rounds(), c.consistentTotals.size(), "Nicht alle Schnappschüsse abgeschlossen");
        for (int total : c.consistentTotals)
            assertEquals(sc.totalMoney(), total, "Schnappschuss muss die Gesamtsumme S erfassen");
    }

    /** Aufgabe 3.2: Der naive Schnappschuss übersieht unterwegs befindliches Geld. */
    @Test
    void aufgabe3_naiverSchnappschussIstInkonsistent() {
        Scenario sc = Scenario.of(6, 6, 300, 10, 50, 100, 300);
        Coordinator c = run(sc);
        assertEquals(sc.rounds(), c.naiveTotals.size(), "Nicht alle naiven Schnappschüsse abgeschlossen");
        // Bei diesem Verkehrsaufkommen ist praktisch immer Geld unterwegs, das der naive Ansatz verliert.
        assertTrue(c.naiveTotals.stream().anyMatch(t -> t != sc.totalMoney()),
                "Naiver Schnappschuss traf zufällig immer S: " + c.naiveTotals);
        for (int total : c.consistentTotals)
            assertEquals(sc.totalMoney(), total, "Der eingefärbte Schnappschuss bleibt dabei konsistent");
    }

    /** Aufgabe 3.3: Statistik über n und Überweisungsfrequenz. */
    @Test
    void aufgabe3_statistik() {
        System.out.println("n  | Takt [ms] | Kontrollnachr./Schnappschuss | Kanalnachr./Schnappschuss");
        for (int[] cfg : new int[][]{{3, 10, 50}, {6, 10, 50}, {10, 10, 50}, {6, 80, 200}}) {
            Scenario sc = Scenario.of(cfg[0], 3, 300, cfg[1], cfg[2], 100, 300);
            Coordinator c = run(sc);
            assertEquals(sc.rounds(), c.consistentTotals.size(), "Nicht alle Schnappschüsse abgeschlossen");
            for (int total : c.consistentTotals)
                assertEquals(sc.totalMoney(), total);
            System.out.printf("%-2d | %3d–%-3d   | %28.1f | %25.1f%n", sc.n(), cfg[1], cfg[2],
                    c.controlMessages.stream().mapToInt(Integer::intValue).average().orElse(0),
                    c.channelMessages.stream().mapToInt(Integer::intValue).average().orElse(0));
        }
    }
}
