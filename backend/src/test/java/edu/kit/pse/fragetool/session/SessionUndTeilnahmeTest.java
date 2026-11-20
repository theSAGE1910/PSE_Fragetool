package edu.kit.pse.fragetool.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.kit.pse.fragetool.support.ApiTestSupport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

/** Sessions und Teilnahme: /MK130/, /MK140/, /MK160/ bis /MK180/ (Testfälle /T90/, /T95/, /T100/, /T110/). */
class SessionUndTeilnahmeTest extends ApiTestSupport {

    @Autowired
    private ParticipantService participantService;

    @Test
    @DisplayName("/MK130/, /MK140/ Session anlegen, benennen, löschen; Link gilt, solange die Session existiert")
    void sessionLebenszyklus() throws Exception {
        MockHttpSession dozent = newDozent();
        String session = createSession(dozent, "Rechnernetze VL 05");
        String id = read(session, "$.id");
        String code = read(session, "$.joinCode");
        assertTrue(code.matches(NAME_CHARS + "{6}"), code);
        assertEquals("Rechnernetze VL 05", read(session, "$.name"));

        String other = read(createSession(dozent, "Rechnernetze VL 06"), "$.joinCode");
        assertNotEquals(code, other);

        mvc.perform(json(post("/api/sessions"), "{\"name\":\"   \"}").session(dozent))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/sessions"), "{\"name\":\"" + "x".repeat(201) + "\"}").session(dozent))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/sessions").session(dozent)).andExpect(jsonPath("$.length()").value(2));
        // Der Beitrittscode ändert sich nicht (Link und QR-Code bleiben gültig)
        mvc.perform(get("/api/sessions/" + id).session(dozent)).andExpect(jsonPath("$.joinCode").value(code));
        stateRequest(code).andExpect(status().isOk()).andExpect(jsonPath("$.sessionName").value("Rechnernetze VL 05"));

        Joined alice = join(code);
        mvc.perform(delete("/api/sessions/" + id).session(dozent)).andExpect(status().isNoContent());
        mvc.perform(get("/api/sessions").session(dozent)).andExpect(jsonPath("$.length()").value(1));
        stateRequest(code)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Session nicht gefunden."));
        mvc.perform(post("/api/public/sessions/" + code + "/participants")).andExpect(status().isNotFound());
        // Mit der Session sind auch ihre Teilnehmer gelöscht
        meRequest(alice.token()).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("/MK160/, /MK170/, /MK171/, /MK172/, /MK173/ Beitritt ohne Anmeldung mit automatischem Namen")
    void beitrittMitAutomatischemNamen() throws Exception {
        MockHttpSession dozent = newDozent();
        String code = read(createSession(dozent, "Rechnernetze VL 05"), "$.joinCode");

        Set<String> names = new HashSet<>();
        Joined first = null;
        for (int i = 0; i < 30; i++) {
            Joined joined = join(code);   // ohne Dozenten-Sitzung, also ohne Anmeldung
            if (first == null) {
                first = joined;
            }
            assertTrue(joined.displayName().matches(NAME_CHARS + "{3}"), joined.displayName());
            assertTrue(joined.token().length() >= 32, "Token zu kurz");
            names.add(joined.displayName());
        }
        assertEquals(30, names.size(), "Anzeigenamen müssen in der Session eindeutig sein");
        stateRequest(code).andExpect(jsonPath("$.participantCount").value(30));

        // Bereits vergebene Namen ändern sich nicht
        assertEquals(first.displayName(), read(me(first.token()), "$.displayName"));
        mvc.perform(post("/api/public/sessions/UNBEKANNT/participants")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("/MK173/ Gleichzeitige Beitritte erhalten verschiedene Namen")
    void gleichzeitigeBeitritte() throws Exception {
        MockHttpSession dozent = newDozent();
        String code = read(createSession(dozent, "Große Vorlesung"), "$.joinCode");
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 64; i++) {
                futures.add(pool.submit(() -> participantService.join(code).getDisplayName()));
            }
            Set<String> names = new HashSet<>();
            for (Future<String> future : futures) {
                names.add(future.get());
            }
            assertEquals(64, names.size());
        } finally {
            pool.shutdownNow();
        }
        stateRequest(code).andExpect(jsonPath("$.participantCount").value(64));
    }

    @Test
    @DisplayName("/MK180/ Nach dem Neuladen bleiben Name und Punktestand erhalten")
    void neuladenBehaeltNameUndPunkte() throws Exception {
        MockHttpSession dozent = newDozent();
        String question = createQuestion(dozent, F1);
        String session = createSession(dozent, "Rechnernetze VL 05");
        String code = read(session, "$.joinCode");
        Joined alice = join(code);

        String runId = start(dozent, read(session, "$.id"), read(question, "$.id"));
        answerOption(runId, alice.token(), read(question, "$.options[1].id")).andExpect(status().isCreated());
        close(dozent, runId);

        // "Neuladen": der Browser fragt mit dem gespeicherten Token erneut nach seinen Daten
        for (int reload = 0; reload < 2; reload++) {
            meRequest(alice.token())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value(alice.displayName()))
                    .andExpect(jsonPath("$.points").value(1))
                    .andExpect(jsonPath("$.joinCode").value(code));
        }
        meRequest("unbekanntes-token-" + UUID.randomUUID()).andExpect(status().isNotFound());

        // Ein neuer Beitritt (z. B. privates Fenster ohne Token) ergibt einen neuen Teilnehmer
        Joined again = join(code);
        assertNotEquals(alice.displayName(), again.displayName());
        assertEquals(0, (int) read(me(again.token()), "$.points"));
    }
}
