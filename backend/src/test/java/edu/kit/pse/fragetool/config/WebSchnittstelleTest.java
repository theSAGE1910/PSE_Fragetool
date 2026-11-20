package edu.kit.pse.fragetool.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.kit.pse.fragetool.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/** Querschnitt: direkte Aufrufe von Links (/MK140/, /MK160/, /MK180/) und deutsche Fehlermeldungen (/MK370/). */
class WebSchnittstelleTest extends ApiTestSupport {

    @Test
    @DisplayName("/MK160/, /MK180/ Teilnahme-Link und Präsentationsansicht funktionieren beim direkten Aufruf")
    void linksWerdenAnDieAnwendungWeitergeleitet() throws Exception {
        mvc.perform(get("/join/BCDFGH")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/present/BCDFGH")).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/login")).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/dozent/sessions/123")).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    @DisplayName("/MK370/ Fehlerantworten enthalten eine deutsche Meldung")
    void fehlerMitDeutscherMeldung() throws Exception {
        MockHttpSession dozent = newDozent();
        mvc.perform(json(post("/api/questions"), "{kein json").session(dozent))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Die Anfrage ist ungültig."));
        stateRequest("XXXXXX")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Session nicht gefunden."));
    }
}
