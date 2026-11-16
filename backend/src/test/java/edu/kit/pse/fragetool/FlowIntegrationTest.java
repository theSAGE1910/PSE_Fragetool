package edu.kit.pse.fragetool;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Durchgehender Ablauf wie in Testfallszenario /T120/, über die REST-Schnittstelle:
 * registrieren, Frage und Session anlegen, beitreten, Frage starten, antworten, beenden, Punkte prüfen.
 * Läuft gegen eine H2-Datenbank im Arbeitsspeicher.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FlowIntegrationTest {

    private static final String TOKEN_HEADER = "X-Participant-Token";

    @Autowired
    private MockMvc mvc;

    @Test
    void kompletteAbstimmungMitSingleChoice() throws Exception {
        // /MK10/: registrieren; dieselbe E-Mail-Adresse ein zweites Mal wird abgelehnt
        register("dozent@test.de", "Sicher123!").andExpect(status().isCreated());
        register("dozent@test.de", "Sicher123!").andExpect(status().isConflict());

        // /MK20/, /MK40/: ohne Anmeldung kein Zugriff, falsches Passwort wird abgelehnt
        mvc.perform(get("/api/questions")).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/auth/login"), "{\"email\":\"dozent@test.de\",\"password\":\"falsch\"}"))
                .andExpect(status().isUnauthorized());
        MockHttpSession dozent = login("dozent@test.de", "Sicher123!");

        // /MK50/ bis /MK110/: Single-Choice-Frage F1 erstellen
        String question = mvc.perform(json(post("/api/questions"), """
                        {"type":"SINGLE_CHOICE","text":"Auf welcher OSI-Schicht arbeitet IP?","timeLimitSeconds":30,
                         "tags":["ip"],"options":[{"text":"Schicht 2","correct":false},
                         {"text":"Schicht 3","correct":true},{"text":"Schicht 4","correct":false}]}""").session(dozent))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String questionId = JsonPath.read(question, "$.id");
        String wrongOption = JsonPath.read(question, "$.options[0].id");
        String correctOption = JsonPath.read(question, "$.options[1].id");

        // /MK120/: Filter nach Tag
        mvc.perform(get("/api/questions").param("tag", "ip").session(dozent))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/questions").param("tag", "tcp").session(dozent))
                .andExpect(jsonPath("$.length()").value(0));

        // /MK70/: eine Frage mit nur einer Antwortmöglichkeit wird abgelehnt
        mvc.perform(json(post("/api/questions"), """
                        {"type":"SINGLE_CHOICE","text":"Ungültig","options":[{"text":"A","correct":true}]}""")
                        .session(dozent))
                .andExpect(status().isBadRequest());

        // /MK130/, /MK140/: Session anlegen
        String session = mvc.perform(json(post("/api/sessions"), "{\"name\":\"Rechnernetze VL 05\"}").session(dozent))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionId = JsonPath.read(session, "$.id");
        String joinCode = JsonPath.read(session, "$.joinCode");

        // /MK160/ bis /MK173/: zwei Teilnehmer treten ohne Anmeldung bei und erhalten verschiedene Namen
        String alice = join(joinCode);
        String bob = join(joinCode);
        String aliceToken = JsonPath.read(alice, "$.token");
        String bobToken = JsonPath.read(bob, "$.token");
        String aliceName = JsonPath.read(alice, "$.displayName");
        String bobName = JsonPath.read(bob, "$.displayName");
        assertTrue(aliceName.matches("[BCDFGHJKLMNPQRSTVWXZ2-9]{3}"), aliceName);
        assertNotEquals(aliceName, bobName);

        // /MK190/, /MK210/: Frage starten; eine zweite Frage gleichzeitig ist nicht erlaubt
        String start = "{\"questionId\":\"" + questionId + "\"}";
        String run = mvc.perform(json(post("/api/sessions/" + sessionId + "/runs"), start).session(dozent))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String runId = JsonPath.read(run, "$.runId");
        mvc.perform(json(post("/api/sessions/" + sessionId + "/runs"), start).session(dozent))
                .andExpect(status().isConflict());

        // /MK220/, /MK270/: der öffentliche Zustand zeigt die laufende Frage ohne Lösung
        mvc.perform(get("/api/public/sessions/" + joinCode + "/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.run.status").value("RUNNING"))
                .andExpect(jsonPath("$.run.question.options.length()").value(3))
                .andExpect(jsonPath("$.run.question.options[0].correct").doesNotExist())
                .andExpect(jsonPath("$.run.result").doesNotExist());

        // /MK240/: Alice antwortet richtig, eine zweite Antwort wird abgelehnt; Bob antwortet falsch
        answer(runId, aliceToken, correctOption).andExpect(status().isCreated());
        answer(runId, aliceToken, wrongOption).andExpect(status().isConflict());
        answer(runId, bobToken, wrongOption).andExpect(status().isCreated());

        // /MK260/, /MK255/: vorzeitig beenden; danach werden keine Antworten mehr angenommen
        mvc.perform(post("/api/runs/" + runId + "/close").session(dozent)).andExpect(status().isNoContent());
        String carolToken = JsonPath.read(join(joinCode), "$.token");
        answer(runId, carolToken, correctOption).andExpect(status().isConflict());

        // /MK270/, /MK290/, /MK300/: Lösung und Verteilung nach Fragenende
        mvc.perform(get("/api/public/sessions/" + joinCode + "/state"))
                .andExpect(jsonPath("$.run.status").value("CLOSED"))
                .andExpect(jsonPath("$.run.answerCount").value(2))
                .andExpect(jsonPath("$.run.result.solution").value("Schicht 3"))
                .andExpect(jsonPath("$.run.result.distribution[1].count").value(1))
                .andExpect(jsonPath("$.run.result.distribution[1].correct").value(true));

        // /MK280/, /MK310/, /MK330/: Punkte, Rang und eigenes Ergebnis
        mvc.perform(get("/api/public/participants/me").header(TOKEN_HEADER, aliceToken))
                .andExpect(jsonPath("$.displayName").value(aliceName))
                .andExpect(jsonPath("$.points").value(1))
                .andExpect(jsonPath("$.rank").value(1))
                .andExpect(jsonPath("$.correct").value(true));
        mvc.perform(get("/api/public/participants/me").header(TOKEN_HEADER, bobToken))
                .andExpect(jsonPath("$.points").value(0))
                .andExpect(jsonPath("$.rank").value(2))
                .andExpect(jsonPath("$.correct").value(false));

        // /MK40/: ein zweiter Dozent sieht und erreicht die fremde Frage nicht
        register("zweit@test.de", "Sicher456!").andExpect(status().isCreated());
        MockHttpSession zweit = login("zweit@test.de", "Sicher456!");
        mvc.perform(get("/api/questions").session(zweit)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/questions/" + questionId).session(zweit)).andExpect(status().isForbidden());

        // /MK20/: nach dem Abmelden ist die Sitzung ungültig
        mvc.perform(post("/api/auth/logout").session(dozent)).andExpect(status().isNoContent());
        mvc.perform(get("/api/questions").session(dozent)).andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions register(String email, String password)
            throws Exception {
        return mvc.perform(json(post("/api/auth/register"),
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(json(post("/api/auth/login"),
                        "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }

    private String join(String joinCode) throws Exception {
        return mvc.perform(post("/api/public/sessions/" + joinCode + "/participants"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private org.springframework.test.web.servlet.ResultActions answer(String runId, String token, String optionId)
            throws Exception {
        return mvc.perform(json(post("/api/public/runs/" + runId + "/answers"),
                "{\"optionIds\":[\"" + optionId + "\"]}").header(TOKEN_HEADER, token));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder json(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
