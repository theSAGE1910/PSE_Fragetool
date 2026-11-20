package edu.kit.pse.fragetool.auth;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.kit.pse.fragetool.support.ApiTestSupport;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Dozentenkonto und Zugriffsschutz: /MK10/, /MK20/, /MK30/, /MK40/ (Testfälle /T10/, /T20/, /T30/). */
class DozentKontoTest extends ApiTestSupport {

    @Autowired
    private DozentRepository dozents;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("/MK10/ Registrieren; doppelte oder ungültige Angaben werden abgelehnt")
    void registrieren() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD).andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(email));
        register(email, PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("E-Mail-Adresse bereits vergeben."));
        // Groß- und Kleinschreibung der E-Mail-Adresse spielt keine Rolle
        register(email.toUpperCase(Locale.ROOT), PASSWORD).andExpect(status().isConflict());
        register("keine-adresse", PASSWORD).andExpect(status().isBadRequest());
        register(uniqueEmail(), "kurz").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("/MK30/ Passwörter werden nur als bcrypt-Hash gespeichert")
    void passwortNurAlsHash() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD).andExpect(status().isCreated());
        Dozent dozent = dozents.findByEmail(email).orElseThrow();
        assertNotEquals(PASSWORD, dozent.getPasswordHash());
        assertTrue(dozent.getPasswordHash().startsWith("$2"), "bcrypt-Hash erwartet");
        assertTrue(passwordEncoder.matches(PASSWORD, dozent.getPasswordHash()));
    }

    @Test
    @DisplayName("/MK20/ Anmelden und Abmelden; ohne Anmeldung kein Zugriff")
    void anmeldenUndAbmelden() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD).andExpect(status().isCreated());

        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sessions")).andExpect(status().isUnauthorized());
        loginRequest(email, "falsch")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-Mail oder Passwort falsch."));
        loginRequest(uniqueEmail(), PASSWORD).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/auth/login"), "{\"email\":\"" + email + "\"}"))
                .andExpect(status().isBadRequest());

        MockHttpSession dozent = login(email, PASSWORD);
        mvc.perform(get("/api/auth/me").session(dozent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        mvc.perform(get("/api/sessions").session(dozent)).andExpect(status().isOk());

        mvc.perform(post("/api/auth/logout").session(dozent)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(dozent)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sessions").session(dozent)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/MK40/ Ein Dozent sieht und ändert keine fremden Fragen, Sessions und Fragenläufe")
    void fremdeDatenSindGeschuetzt() throws Exception {
        MockHttpSession a = newDozent();
        String questionId = read(createQuestion(a, F1), "$.id");
        String sessionId = read(createSession(a, "Rechnernetze VL 05"), "$.id");
        String runId = start(a, sessionId, questionId);

        MockHttpSession b = newDozent();
        String ownSessionId = read(createSession(b, "Mathe VL 03"), "$.id");

        mvc.perform(get("/api/questions").session(b)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/sessions").session(b)).andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get("/api/questions/" + questionId).session(b)).andExpect(status().isForbidden());
        mvc.perform(json(put("/api/questions/" + questionId), F1).session(b)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/questions/" + questionId).session(b)).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/questions/" + questionId + "/image")
                        .file(new MockMultipartFile("file", "b.png", "image/png", new byte[] {1, 2, 3}))
                        .session(b))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/sessions/" + sessionId).session(b)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/sessions/" + sessionId).session(b)).andExpect(status().isForbidden());
        setScoreboardVisible(b, sessionId, true).andExpect(status().isForbidden());

        startRequest(b, sessionId, questionId).andExpect(status().isForbidden());
        startRequest(b, ownSessionId, questionId).andExpect(status().isForbidden());
        closeRequest(b, runId).andExpect(status().isForbidden());
        mvc.perform(json(put("/api/runs/" + runId + "/freetext-released"), "{\"released\":true}").session(b))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/runs/" + runId + "/freetext").session(b)).andExpect(status().isForbidden());

        // die Daten von A sind unverändert
        mvc.perform(get("/api/questions/" + questionId).session(a)).andExpect(status().isOk());
        mvc.perform(get("/api/sessions/" + sessionId).session(a)).andExpect(status().isOk());

        // unbekannte IDs
        mvc.perform(get("/api/sessions/" + UUID.randomUUID()).session(a)).andExpect(status().isNotFound());
        mvc.perform(get("/api/questions/" + UUID.randomUUID()).session(a)).andExpect(status().isNotFound());
        closeRequest(a, UUID.randomUUID().toString()).andExpect(status().isNotFound());
    }
}
