package edu.kit.pse.fragetool.support;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Gemeinsame Grundlage der Integrationstests.
 *
 * <p>Startet die vollständige Anwendung mit echtem Webserver (für WebSocket) und einer eigenen
 * H2-Datenbank im Arbeitsspeicher. Alle Testklassen, die von dieser Klasse erben, teilen sich
 * denselben Anwendungskontext. Jeder Test legt eigene Dozenten mit eindeutiger E-Mail-Adresse an,
 * damit sich die Tests nicht gegenseitig beeinflussen.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:fragetool-it;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
public abstract class ApiTestSupport {

    public static final String TOKEN_HEADER = "X-Participant-Token";
    public static final String PASSWORD = "Sicher123!";
    /** Zeichen für Anzeigenamen und Beitrittscodes (/MK171/). */
    public static final String NAME_CHARS = "[BCDFGHJKLMNPQRSTVWXZ2-9]";

    /** Frage F1 aus Abschnitt 8.1 des Pflichtenhefts. */
    public static final String F1 = """
            {"type":"SINGLE_CHOICE","text":"Auf welcher OSI-Schicht arbeitet IP?","timeLimitSeconds":30,
             "tags":["ip"],"options":[{"text":"Schicht 2","correct":false},
             {"text":"Schicht 3","correct":true},{"text":"Schicht 4","correct":false}]}""";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected MutableClock clock;

    /** Teilnehmer, wie ihn der Browser nach dem Beitritt kennt. */
    public record Joined(String displayName, String token) {
    }

    @BeforeEach
    protected void resetClock() {
        clock.set(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    }

    // ---------------------------------------------------------------- Dozent

    protected static String uniqueEmail() {
        return "dozent-" + UUID.randomUUID() + "@test.de";
    }

    protected ResultActions register(String email, String password) throws Exception {
        return mvc.perform(json(post("/api/auth/register"),
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    protected ResultActions loginRequest(String email, String password) throws Exception {
        return mvc.perform(json(post("/api/auth/login"),
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    protected MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = loginRequest(email, password).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }

    /** Registriert einen neuen Dozenten und meldet ihn an. */
    protected MockHttpSession newDozent() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD).andExpect(status().isCreated());
        return login(email, PASSWORD);
    }

    // ---------------------------------------------------------------- Katalog und Sessions

    protected ResultActions postQuestion(MockHttpSession dozent, String body) throws Exception {
        return mvc.perform(json(post("/api/questions"), body).session(dozent));
    }

    /** Legt eine gültige Frage an und liefert die Antwort des Servers als JSON. */
    protected String createQuestion(MockHttpSession dozent, String body) throws Exception {
        return postQuestion(dozent, body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected String createSession(MockHttpSession dozent, String name) throws Exception {
        return mvc.perform(json(post("/api/sessions"), "{\"name\":\"" + name + "\"}").session(dozent))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------- Durchführung

    protected ResultActions startRequest(MockHttpSession dozent, String sessionId, String questionId)
            throws Exception {
        return mvc.perform(json(post("/api/sessions/" + sessionId + "/runs"),
                "{\"questionId\":\"" + questionId + "\"}").session(dozent));
    }

    /** Startet eine Katalogfrage und liefert die ID des Fragenlaufs. */
    protected String start(MockHttpSession dozent, String sessionId, String questionId) throws Exception {
        String body = startRequest(dozent, sessionId, questionId).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return read(body, "$.runId");
    }

    protected ResultActions quickStartRequest(MockHttpSession dozent, String sessionId, String body)
            throws Exception {
        return mvc.perform(json(post("/api/sessions/" + sessionId + "/quickstart"), body).session(dozent));
    }

    protected ResultActions closeRequest(MockHttpSession dozent, String runId) throws Exception {
        return mvc.perform(post("/api/runs/" + runId + "/close").session(dozent));
    }

    protected void close(MockHttpSession dozent, String runId) throws Exception {
        closeRequest(dozent, runId).andExpect(status().isNoContent());
    }

    protected ResultActions setScoreboardVisible(MockHttpSession dozent, String sessionId, boolean visible)
            throws Exception {
        return mvc.perform(json(put("/api/sessions/" + sessionId + "/scoreboard-visible"),
                "{\"visible\":" + visible + "}").session(dozent));
    }

    // ---------------------------------------------------------------- Teilnehmer

    protected Joined join(String joinCode) throws Exception {
        String body = mvc.perform(post("/api/public/sessions/" + joinCode + "/participants"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new Joined(read(body, "$.displayName"), read(body, "$.token"));
    }

    protected ResultActions answer(String runId, String token, String body) throws Exception {
        return mvc.perform(json(post("/api/public/runs/" + runId + "/answers"), body).header(TOKEN_HEADER, token));
    }

    /** Antwort mit einer oder mehreren Antwortmöglichkeiten. */
    protected ResultActions answerOption(String runId, String token, String optionId, String... moreOptionIds)
            throws Exception {
        StringBuilder ids = new StringBuilder("\"").append(optionId).append('"');
        for (String id : moreOptionIds) {
            ids.append(",\"").append(id).append('"');
        }
        return answer(runId, token, "{\"optionIds\":[" + ids + "]}");
    }

    protected ResultActions stateRequest(String joinCode) throws Exception {
        return mvc.perform(get("/api/public/sessions/" + joinCode + "/state"));
    }

    protected String state(String joinCode) throws Exception {
        return stateRequest(joinCode).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected ResultActions meRequest(String token) throws Exception {
        return mvc.perform(get("/api/public/participants/me").header(TOKEN_HEADER, token));
    }

    protected String me(String token) throws Exception {
        return meRequest(token).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------- Hilfen

    protected static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected static <T> T read(String json, String path) {
        return JsonPath.read(json, path);
    }

    protected static Instant instant(String json, String path) {
        return Instant.parse(JsonPath.<String>read(json, path));
    }
}
