package edu.kit.pse.fragetool.live;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import edu.kit.pse.fragetool.support.ApiTestSupport;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * Benachrichtigungen per WebSocket mit einem echten STOMP-Client:
 * /MK220/ (ohne Neuladen), /MK400/ (innerhalb einer Sekunde), /MK410/ (Heartbeat des Servers).
 */
class LiveBenachrichtigungTest extends ApiTestSupport {

    /** Grenze aus /MK400/. */
    private static final Duration MAX_DELAY = Duration.ofSeconds(1);

    @Autowired
    private Environment environment;

    private ThreadPoolTaskScheduler clientScheduler;
    private WebSocketStompClient client;
    private StompSession stomp;
    private StompHeaders connectedHeaders;

    /** Eine empfangene Nachricht mit Empfangszeitpunkt. */
    private record Received(String json, long nanos) {
        String type() {
            return JsonPath.read(json, "$.type");
        }
    }

    @BeforeEach
    void connect() throws Exception {
        clientScheduler = new ThreadPoolTaskScheduler();
        clientScheduler.initialize();
        client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setTaskScheduler(clientScheduler);
        client.setDefaultHeartbeat(new long[] {10_000, 10_000});
        CompletableFuture<StompHeaders> connected = new CompletableFuture<>();
        String url = "ws://localhost:" + environment.getProperty("local.server.port") + "/ws";
        stomp = client.connectAsync(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders headers) {
                connected.complete(headers);
            }
        }).get(5, TimeUnit.SECONDS);
        connectedHeaders = connected.get(5, TimeUnit.SECONDS);
    }

    @AfterEach
    void disconnect() {
        if (stomp != null && stomp.isConnected()) {
            stomp.disconnect();
        }
        client.stop();
        clientScheduler.shutdown();
    }

    @Test
    @DisplayName("/MK220/, /MK400/ Start, Antworten, Beitritte und Ende erreichen die Ansichten innerhalb von 1 s")
    void benachrichtigungen() throws Exception {
        MockHttpSession dozent = newDozent();
        String question = createQuestion(dozent, F1);
        String session = createSession(dozent, "Rechnernetze VL 05");
        String sessionId = read(session, "$.id");
        String code = read(session, "$.joinCode");
        BlockingQueue<Received> inbox = subscribe(code);

        Joined alice = join(code);
        int participants = read(state(code), "$.participantCount");
        nextWithValue(inbox, "PARTICIPANT_COUNT", participants);

        long before = System.nanoTime();
        String runId = start(dozent, sessionId, read(question, "$.id"));
        Received started = next(inbox, "STATE_CHANGED");
        assertDelay(before, started);
        assertNotNull(JsonPath.read(started.json(), "$.serverNow"));

        answerOption(runId, alice.token(), read(question, "$.options[1].id")).andExpect(status().isCreated());
        nextWithValue(inbox, "ANSWER_COUNT", 1);

        before = System.nanoTime();
        close(dozent, runId);
        assertDelay(before, next(inbox, "STATE_CHANGED"));

        // Freigabe von Freitext und Scoreboard lösen ebenfalls ein Neuladen aus
        setScoreboardVisible(dozent, sessionId, true).andExpect(status().isNoContent());
        next(inbox, "STATE_CHANGED");
    }

    @Test
    @DisplayName("/MK220/ Ansichten erhalten nur Nachrichten ihrer eigenen Session")
    void nurEigeneSession() throws Exception {
        MockHttpSession dozent = newDozent();
        String code = read(createSession(dozent, "Rechnernetze VL 05"), "$.joinCode");
        String otherId = read(createSession(dozent, "Mathe VL 03"), "$.id");
        BlockingQueue<Received> inbox = subscribe(code);

        quickStartRequest(dozent, otherId, "{\"type\":\"FREE_TEXT\"}").andExpect(status().isCreated());
        assertNoStateMessage(inbox, "Nachricht einer fremden Session empfangen");
    }

    @Test
    @DisplayName("Nachrichten von Clients werden verworfen (Aktionen laufen nur über REST)")
    void clientNachrichtenWerdenVerworfen() throws Exception {
        MockHttpSession dozent = newDozent();
        String code = read(createSession(dozent, "Rechnernetze VL 05"), "$.joinCode");
        BlockingQueue<Received> inbox = subscribe(code);

        stomp.send("/topic/session/" + code, "{\"type\":\"STATE_CHANGED\"}".getBytes(StandardCharsets.UTF_8));
        assertNoStateMessage(inbox, "Nachricht eines Clients wurde weitergeleitet");
    }

    @Test
    @DisplayName("/MK410/ Der Server sendet Heartbeats, damit Verbindungsabbrüche erkannt werden")
    void heartbeat() {
        assertArrayEquals(new long[] {10_000, 10_000}, connectedHeaders.getHeartbeat());
    }

    /**
     * Abonniert die Session und wartet, bis das Abonnement beim Broker aktiv ist.
     * Dazu treten so lange Prüf-Teilnehmer bei, bis die erste Nachricht ankommt.
     */
    private BlockingQueue<Received> subscribe(String code) throws Exception {
        BlockingQueue<Received> inbox = new LinkedBlockingQueue<>();
        stomp.subscribe(LiveSessionPublisher.topic(code), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                inbox.add(new Received(new String((byte[]) payload, StandardCharsets.UTF_8), System.nanoTime()));
            }
        });
        // Prüfen, dass das Abonnement aktiv ist: ein Beitritt muss ankommen
        for (int attempt = 0; attempt < 50; attempt++) {
            join(code);
            if (inbox.poll(100, TimeUnit.MILLISECONDS) != null) {
                Thread.sleep(100);
                inbox.clear();
                return inbox;
            }
        }
        throw new AssertionError("Abonnement wurde nicht aktiv");
    }

    /** Wartet höchstens 2 s auf die nächste Nachricht des erwarteten Typs. */
    private static Received next(BlockingQueue<Received> inbox, String type) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            Received message = inbox.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
            if (message != null && type.equals(message.type())) {
                return message;
            }
        }
        throw new AssertionError("Keine Nachricht vom Typ " + type + " empfangen");
    }

    /** Wartet auf eine Nachricht des Typs mit dem erwarteten Zählerstand (ältere Zählerstände werden übersprungen). */
    private static void nextWithValue(BlockingQueue<Received> inbox, String type, int expected)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            Received message = inbox.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
            if (message != null && type.equals(message.type())
                    && expected == ((Number) JsonPath.read(message.json(), "$.value")).intValue()) {
                return;
            }
        }
        throw new AssertionError("Keine Nachricht " + type + " mit Wert " + expected + " empfangen");
    }

    /**
     * Prüft, dass 500 ms lang keine Nachricht außer verspäteten Beitrittszählern
     * aus {@link #subscribe(String)} ankommt.
     */
    private static void assertNoStateMessage(BlockingQueue<Received> inbox, String reason)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
        while (System.nanoTime() < deadline) {
            Received message = inbox.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
            assertTrue(message == null || "PARTICIPANT_COUNT".equals(message.type()), reason);
        }
    }

    private static void assertDelay(long startNanos, Received message) {
        Duration delay = Duration.ofNanos(message.nanos() - startNanos);
        assertTrue(delay.compareTo(MAX_DELAY) <= 0, "Verzögerung " + delay.toMillis() + " ms > 1000 ms");
    }
}
