package edu.kit.pse.fragetool.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.kit.pse.fragetool.support.ApiTestSupport;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/**
 * Durchführung und Auswertung: /MK190/ bis /MK310/ und /MK430/
 * (Testfälle /T120/, /T130/, /T140/, /T150/, /T160/, /T170/, /T175/, /T180/, /T185/).
 */
class FragendurchfuehrungTest extends ApiTestSupport {

    private static final String MC = """
            {"type":"MULTIPLE_CHOICE","text":"Welche Protokolle arbeiten auf Schicht 4?","timeLimitSeconds":60,
             "options":[{"text":"TCP","correct":true},{"text":"UDP","correct":true},{"text":"IP","correct":false}]}""";
    private static final String NUMERIC = """
            {"type":"NUMERIC","text":"Wie viele Millisekunden?","timeLimitSeconds":60,"correctValue":100,"tolerance":5}""";
    private static final String FREE_TEXT = """
            {"type":"FREE_TEXT","text":"Was macht ein Router?","timeLimitSeconds":60}""";

    private MockHttpSession dozent;
    private String sessionId;
    private String code;

    @BeforeEach
    void neueSession() throws Exception {
        dozent = newDozent();
        String session = createSession(dozent, "Rechnernetze VL 05");
        sessionId = read(session, "$.id");
        code = read(session, "$.joinCode");
    }

    @Test
    @DisplayName("/MK190/, /MK230/, /MK430/ Katalogfrage starten; Start, Ende und Uhrzeit kommen vom Server")
    void katalogfrageStarten() throws Exception {
        String question = createQuestion(dozent, F1);
        Instant t0 = clock.instant();
        String runId = start(dozent, sessionId, read(question, "$.id"));

        String state = state(code);
        assertEquals(runId, read(state, "$.run.id"));
        assertEquals("RUNNING", read(state, "$.run.status"));
        assertEquals("Auf welcher OSI-Schicht arbeitet IP?", read(state, "$.run.question.text"));
        assertEquals(t0, instant(state, "$.run.startedAt"));
        assertEquals(t0.plusSeconds(30), instant(state, "$.run.endsAt"));
        assertEquals(t0, instant(state, "$.serverNow"));

        clock.advance(Duration.ofSeconds(12));
        assertEquals(t0.plusSeconds(12), instant(state(code), "$.serverNow"));

        mvc.perform(json(post("/api/sessions/" + sessionId + "/runs"), "{}").session(dozent))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("/MK200/ Schnellstart-Fragen: Typ, Anzahl der Antworten und Zeit wählbar, nicht bewertet")
    void schnellstart() throws Exception {
        String runId = quickStart("{\"type\":\"SINGLE_CHOICE\",\"optionCount\":3,\"timeLimitSeconds\":20}");
        String state = state(code);
        assertEquals(true, read(state, "$.run.question.quickStart"));
        assertEquals(false, read(state, "$.run.question.gradable"));
        assertEquals("Schnellstart-Frage", read(state, "$.run.question.text"));
        assertEquals(List.of("A", "B", "C"), read(state, "$.run.question.options[*].text"));
        assertEquals(Duration.ofSeconds(20), Duration.between(
                instant(state, "$.run.startedAt"), instant(state, "$.run.endsAt")));

        Joined alice = join(code);
        answerOption(runId, alice.token(), read(state, "$.run.question.options[0].id"))
                .andExpect(status().isCreated());
        close(dozent, runId);
        stateRequest(code)
                .andExpect(jsonPath("$.run.status").value("CLOSED"))
                .andExpect(jsonPath("$.run.result.solution").doesNotExist())
                .andExpect(jsonPath("$.run.result.distribution[0].count").value(1))
                .andExpect(jsonPath("$.run.result.distribution[0].correct").doesNotExist());
        meRequest(alice.token())
                .andExpect(jsonPath("$.points").value(0))
                .andExpect(jsonPath("$.answered").value(true))
                .andExpect(jsonPath("$.correct").doesNotExist());

        quickStartRequest(dozent, sessionId, "{\"optionCount\":3}").andExpect(status().isBadRequest());
        quickStartRequest(dozent, sessionId, "{\"type\":\"SINGLE_CHOICE\",\"optionCount\":1}")
                .andExpect(status().isBadRequest());
        quickStartRequest(dozent, sessionId, "{\"type\":\"SINGLE_CHOICE\",\"optionCount\":7}")
                .andExpect(status().isBadRequest());
        quickStartRequest(dozent, sessionId, "{\"type\":\"SINGLE_CHOICE\",\"timeLimitSeconds\":5}")
                .andExpect(status().isBadRequest());

        // Standardwerte: 4 Antwortmöglichkeiten, 45 Sekunden
        String mc = quickStart("{\"type\":\"MULTIPLE_CHOICE\"}");
        String mcState = state(code);
        assertEquals(4, (int) read(mcState, "$.run.question.options.length()"));
        assertEquals(Duration.ofSeconds(45), Duration.between(
                instant(mcState, "$.run.startedAt"), instant(mcState, "$.run.endsAt")));
        close(dozent, mc);

        String numeric = quickStart("{\"type\":\"NUMERIC\"}");
        answer(numeric, alice.token(), "{\"numericValue\":42}").andExpect(status().isCreated());
        close(dozent, numeric);

        String text = quickStart("{\"type\":\"FREE_TEXT\"}");
        answer(text, alice.token(), "{\"text\":\"Alles klar\"}").andExpect(status().isCreated());
        close(dozent, text);
        assertEquals(0, (int) read(me(alice.token()), "$.points"));
    }

    @Test
    @DisplayName("/MK210/ Je Session läuft höchstens eine Frage; andere Sessions sind unabhängig")
    void eineFrageJeSession() throws Exception {
        String questionId = read(createQuestion(dozent, F1), "$.id");
        String runId = start(dozent, sessionId, questionId);
        startRequest(dozent, sessionId, questionId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Es läuft bereits eine Frage."));
        quickStartRequest(dozent, sessionId, "{\"type\":\"FREE_TEXT\"}").andExpect(status().isConflict());
        stateRequest(code).andExpect(jsonPath("$.run.id").value(runId));

        // zweite Session desselben Dozenten und Session eines anderen Dozenten laufen parallel
        String ownSecond = read(createSession(dozent, "Rechnernetze Übung"), "$.id");
        startRequest(dozent, ownSecond, questionId).andExpect(status().isCreated());
        MockHttpSession zweit = newDozent();
        String otherSession = read(createSession(zweit, "Mathe VL 03"), "$.id");
        quickStartRequest(zweit, otherSession, "{\"type\":\"FREE_TEXT\"}").andExpect(status().isCreated());

        close(dozent, runId);
        startRequest(dozent, sessionId, questionId).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("/MK240/, /MK245/, /MK300/ Jede Antwort wird gespeichert, gezählt und nur einmal angenommen")
    void antwortenAbgeben() throws Exception {
        String question = createQuestion(dozent, F1);
        String wrong = read(question, "$.options[0].id");
        String correct = read(question, "$.options[1].id");
        String runId = start(dozent, sessionId, read(question, "$.id"));
        Joined alice = join(code);
        Joined bob = join(code);
        Joined carol = join(code);

        answerOption(runId, alice.token(), correct).andExpect(status().isCreated());
        stateRequest(code).andExpect(jsonPath("$.run.answerCount").value(1));
        answerOption(runId, alice.token(), wrong)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Du hast diese Frage bereits beantwortet."));
        answerOption(runId, bob.token(), wrong).andExpect(status().isCreated());
        stateRequest(code).andExpect(jsonPath("$.run.answerCount").value(2));

        answerOption(runId, carol.token(), UUID.randomUUID().toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Die Antwort gehört nicht zu dieser Frage."));
        answerOption(runId, carol.token(), wrong, correct)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bei Single Choice ist genau eine Antwort erlaubt."));
        answer(runId, carol.token(), "{\"optionIds\":[]}").andExpect(status().isBadRequest());
        answerOption(runId, "unbekannt", correct).andExpect(status().isNotFound());
        answerOption(UUID.randomUUID().toString(), carol.token(), correct).andExpect(status().isNotFound());

        // Teilnehmer einer anderen Session darf nicht antworten
        String otherCode = read(createSession(dozent, "Andere Session"), "$.joinCode");
        answerOption(runId, join(otherCode).token(), correct).andExpect(status().isForbidden());

        stateRequest(code).andExpect(jsonPath("$.run.answerCount").value(2));
    }

    @Test
    @DisplayName("/MK250/ Nach Ablauf des Zeitlimits lehnt der Server Antworten ab (Serverzeit)")
    void zeitablauf() throws Exception {
        String question = createQuestion(dozent, F1);
        String correct = read(question, "$.options[1].id");
        Instant t0 = clock.instant();
        String runId = start(dozent, sessionId, read(question, "$.id"));
        Joined alice = join(code);
        Joined bob = join(code);
        Joined carol = join(code);

        clock.set(t0.plusSeconds(30).minusMillis(1));
        answerOption(runId, alice.token(), correct).andExpect(status().isCreated());
        clock.set(t0.plusSeconds(30));
        answerOption(runId, bob.token(), correct)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Zeit abgelaufen."));
        clock.set(t0.plusSeconds(45));
        answerOption(runId, carol.token(), correct).andExpect(status().isConflict());
        stateRequest(code).andExpect(jsonPath("$.run.answerCount").value(1));
    }

    @Test
    @DisplayName("/MK255/, /MK260/ Vorzeitig beenden; danach werden keine Antworten angenommen")
    void vorzeitigBeenden() throws Exception {
        String question = createQuestion(dozent, F1);
        String correct = read(question, "$.options[1].id");
        String runId = start(dozent, sessionId, read(question, "$.id"));
        Joined alice = join(code);
        Joined bob = join(code);
        answerOption(runId, alice.token(), correct).andExpect(status().isCreated());

        close(dozent, runId);
        stateRequest(code).andExpect(jsonPath("$.run.status").value("CLOSED"));
        answerOption(runId, bob.token(), correct)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Frage beendet."));
        // ein zweites Beenden ändert nichts, Punkte werden nicht doppelt vergeben
        close(dozent, runId);
        meRequest(alice.token()).andExpect(jsonPath("$.points").value(1));
    }

    @Test
    @DisplayName("/MK270/, /MK280/ Lösung und eigenes Ergebnis erst nach Fragenende")
    void loesungErstNachEnde() throws Exception {
        String question = createQuestion(dozent, F1);
        String wrong = read(question, "$.options[0].id");
        String correct = read(question, "$.options[1].id");
        String runId = start(dozent, sessionId, read(question, "$.id"));
        Joined alice = join(code);
        Joined bob = join(code);
        Joined carol = join(code);
        answerOption(runId, alice.token(), correct).andExpect(status().isCreated());
        answerOption(runId, bob.token(), wrong).andExpect(status().isCreated());

        stateRequest(code)
                .andExpect(jsonPath("$.run.question.options[0].correct").doesNotExist())
                .andExpect(jsonPath("$.run.question.options[1].correct").doesNotExist())
                .andExpect(jsonPath("$.run.result").doesNotExist());
        meRequest(alice.token())
                .andExpect(jsonPath("$.answered").value(true))
                .andExpect(jsonPath("$.correct").doesNotExist());

        close(dozent, runId);
        stateRequest(code)
                .andExpect(jsonPath("$.run.result.solution").value("Schicht 3"))
                .andExpect(jsonPath("$.run.result.distribution[0].correct").value(false))
                .andExpect(jsonPath("$.run.result.distribution[1].correct").value(true));
        meRequest(alice.token()).andExpect(jsonPath("$.correct").value(true)).andExpect(jsonPath("$.points").value(1));
        meRequest(bob.token()).andExpect(jsonPath("$.correct").value(false)).andExpect(jsonPath("$.points").value(0));
        meRequest(carol.token())
                .andExpect(jsonPath("$.answered").value(false))
                .andExpect(jsonPath("$.correct").doesNotExist());
    }

    @Test
    @DisplayName("/MK290/, /MK310/ Multiple Choice: Verteilung je Antwort, Punkt nur bei exakt richtiger Auswahl")
    void multipleChoice() throws Exception {
        String question = createQuestion(dozent, MC);
        String tcp = read(question, "$.options[0].id");
        String udp = read(question, "$.options[1].id");
        String ip = read(question, "$.options[2].id");
        String runId = start(dozent, sessionId, read(question, "$.id"));
        Joined alice = join(code);
        Joined bob = join(code);
        Joined carol = join(code);
        answerOption(runId, alice.token(), tcp, udp).andExpect(status().isCreated());
        answerOption(runId, bob.token(), tcp).andExpect(status().isCreated());
        answerOption(runId, carol.token(), tcp, udp, ip).andExpect(status().isCreated());
        close(dozent, runId);

        stateRequest(code)
                .andExpect(jsonPath("$.run.result.solution").value("TCP, UDP"))
                .andExpect(jsonPath("$.run.result.distribution[0].label").value("TCP"))
                .andExpect(jsonPath("$.run.result.distribution[0].count").value(3))
                .andExpect(jsonPath("$.run.result.distribution[1].count").value(2))
                .andExpect(jsonPath("$.run.result.distribution[2].count").value(1))
                .andExpect(jsonPath("$.run.result.distribution[2].correct").value(false));
        meRequest(alice.token()).andExpect(jsonPath("$.points").value(1));
        meRequest(bob.token()).andExpect(jsonPath("$.points").value(0));
        meRequest(carol.token()).andExpect(jsonPath("$.points").value(0));
    }

    @Test
    @DisplayName("/MK80/, /MK290/, /MK310/ Numerische Frage: Toleranzgrenzen zählen als richtig")
    void numerischeFrage() throws Exception {
        String runId = start(dozent, sessionId, read(createQuestion(dozent, NUMERIC), "$.id"));
        Joined alice = join(code);
        Joined bob = join(code);
        Joined carol = join(code);
        Joined dave = join(code);
        answer(runId, alice.token(), "{\"numericValue\":105}").andExpect(status().isCreated());
        answer(runId, bob.token(), "{\"numericValue\":94.99}").andExpect(status().isCreated());
        answer(runId, carol.token(), "{\"numericValue\":95}").andExpect(status().isCreated());
        answer(runId, dave.token(), "{\"text\":\"hundert\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bitte eine Zahl eingeben."));
        answer(runId, dave.token(), "{\"numericValue\":100.0}").andExpect(status().isCreated());
        close(dozent, runId);

        stateRequest(code)
                .andExpect(jsonPath("$.run.result.solution").value("100 ± 5"))
                .andExpect(jsonPath("$.run.result.distribution.length()").value(4))
                .andExpect(jsonPath("$.run.result.distribution[0].label").value("94.99"))
                .andExpect(jsonPath("$.run.result.distribution[0].correct").value(false))
                .andExpect(jsonPath("$.run.result.distribution[1].label").value("95"))
                .andExpect(jsonPath("$.run.result.distribution[1].correct").value(true))
                .andExpect(jsonPath("$.run.result.distribution[2].label").value("100"))
                .andExpect(jsonPath("$.run.result.distribution[3].label").value("105"))
                .andExpect(jsonPath("$.run.result.distribution[3].correct").value(true));
        meRequest(alice.token()).andExpect(jsonPath("$.points").value(1));
        meRequest(bob.token()).andExpect(jsonPath("$.points").value(0));
        meRequest(carol.token()).andExpect(jsonPath("$.points").value(1));
        meRequest(dave.token()).andExpect(jsonPath("$.points").value(1));
    }

    @Test
    @DisplayName("/MK90/, /MK290/, /MK295/ Freitext: nicht bewertet, Liste nur nach Freigabe sichtbar")
    void freitext() throws Exception {
        String runId = start(dozent, sessionId, read(createQuestion(dozent, FREE_TEXT), "$.id"));
        Joined alice = join(code);
        Joined bob = join(code);
        Joined carol = join(code);
        answer(runId, alice.token(), "{\"text\":\"Routing\"}").andExpect(status().isCreated());
        clock.advance(Duration.ofSeconds(1));
        answer(runId, bob.token(), "{\"text\":\"Weiterleitung von Paketen\"}").andExpect(status().isCreated());
        answer(runId, carol.token(), "{\"text\":\"  \"}").andExpect(status().isBadRequest());
        answer(runId, carol.token(), "{\"text\":\"" + "x".repeat(501) + "\"}").andExpect(status().isBadRequest());
        close(dozent, runId);

        stateRequest(code)
                .andExpect(jsonPath("$.run.question.gradable").value(false))
                .andExpect(jsonPath("$.run.freeTextReleased").value(false))
                .andExpect(jsonPath("$.run.result.solution").doesNotExist())
                .andExpect(jsonPath("$.run.result.distribution.length()").value(0));
        // der Dozent sieht die Antworten immer
        mvc.perform(get("/api/runs/" + runId + "/freetext").session(dozent))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("Routing"))
                .andExpect(jsonPath("$[1]").value("Weiterleitung von Paketen"));

        release(runId, true);
        stateRequest(code)
                .andExpect(jsonPath("$.run.freeTextReleased").value(true))
                .andExpect(jsonPath("$.run.result.distribution.length()").value(2))
                .andExpect(jsonPath("$.run.result.distribution[0].label").value("Routing"))
                .andExpect(jsonPath("$.run.result.distribution[0].correct").doesNotExist());
        release(runId, false);
        stateRequest(code).andExpect(jsonPath("$.run.result.distribution.length()").value(0));

        meRequest(alice.token())
                .andExpect(jsonPath("$.points").value(0))
                .andExpect(jsonPath("$.correct").doesNotExist());
    }

    @Test
    @DisplayName("/MK310/ Punkte summieren sich über mehrere Fragen")
    void punkteUeberMehrereFragen() throws Exception {
        String question = createQuestion(dozent, F1);
        String questionId = read(question, "$.id");
        String wrong = read(question, "$.options[0].id");
        String correct = read(question, "$.options[1].id");
        Joined alice = join(code);
        Joined bob = join(code);

        String first = start(dozent, sessionId, questionId);
        answerOption(first, alice.token(), correct).andExpect(status().isCreated());
        answerOption(first, bob.token(), correct).andExpect(status().isCreated());
        close(dozent, first);
        clock.advance(Duration.ofSeconds(1));
        String second = start(dozent, sessionId, questionId);
        answerOption(second, alice.token(), correct).andExpect(status().isCreated());
        answerOption(second, bob.token(), wrong).andExpect(status().isCreated());
        close(dozent, second);

        meRequest(alice.token()).andExpect(jsonPath("$.points").value(2)).andExpect(jsonPath("$.rank").value(1));
        meRequest(bob.token()).andExpect(jsonPath("$.points").value(1)).andExpect(jsonPath("$.rank").value(2));
    }

    @Test
    @DisplayName("/MK430/ Zeitpunkte und Punkte vom Client werden nicht übernommen")
    void clientGibtKeineZeitenUndPunkteVor() throws Exception {
        String question = createQuestion(dozent, F1);
        String correct = read(question, "$.options[1].id");
        Instant t0 = clock.instant();
        String runId = start(dozent, sessionId, read(question, "$.id"));
        Joined alice = join(code);
        Joined bob = join(code);

        answer(runId, alice.token(), "{\"optionIds\":[\"" + correct + "\"],\"points\":100,\"correct\":false,"
                + "\"submittedAt\":\"2000-01-01T00:00:00Z\"}").andExpect(status().isCreated());
        clock.set(t0.plusSeconds(31));
        answer(runId, bob.token(), "{\"optionIds\":[\"" + correct + "\"],"
                + "\"submittedAt\":\"" + t0.plusSeconds(1) + "\"}").andExpect(status().isConflict());
        close(dozent, runId);

        meRequest(alice.token()).andExpect(jsonPath("$.points").value(1)).andExpect(jsonPath("$.correct").value(true));
        meRequest(bob.token()).andExpect(jsonPath("$.points").value(0));
    }

    private String quickStart(String body) throws Exception {
        // Die Testuhr steht still. Ohne Vorspulen hätten mehrere Fragen denselben Startzeitpunkt,
        // und "die zuletzt gestartete Frage" wäre nicht eindeutig.
        clock.advance(Duration.ofSeconds(1));
        String response = quickStartRequest(dozent, sessionId, body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return read(response, "$.runId");
    }

    private void release(String runId, boolean released) throws Exception {
        mvc.perform(json(put("/api/runs/" + runId + "/freetext-released"), "{\"released\":" + released + "}")
                        .session(dozent))
                .andExpect(status().isNoContent());
    }
}
