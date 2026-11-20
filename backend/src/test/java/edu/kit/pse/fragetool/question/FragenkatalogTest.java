package edu.kit.pse.fragetool.question;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.kit.pse.fragetool.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/** Fragenkatalog: /MK50/ bis /MK120/ (Testfälle /T40/, /T50/, /T60/, /T80/). */
class FragenkatalogTest extends ApiTestSupport {

    private static final String MC = """
            {"type":"MULTIPLE_CHOICE","text":"Welche Protokolle arbeiten auf Schicht 4?","timeLimitSeconds":60,
             "options":[{"text":"TCP","correct":true},{"text":"UDP","correct":true},{"text":"IP","correct":false}]}""";
    private static final String NUMERIC = """
            {"type":"NUMERIC","text":"Wie viele Bits hat eine IPv4-Adresse?","correctValue":32,"tolerance":0}""";
    private static final String FREE_TEXT = """
            {"type":"FREE_TEXT","text":"Was war heute unklar?"}""";

    @Test
    @DisplayName("/MK50/, /MK60/ Alle vier Fragetypen anlegen")
    void alleFragetypenAnlegen() throws Exception {
        MockHttpSession dozent = newDozent();
        postQuestion(dozent, F1).andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("SINGLE_CHOICE"))
                .andExpect(jsonPath("$.options.length()").value(3))
                .andExpect(jsonPath("$.options[1].correct").value(true))
                .andExpect(jsonPath("$.timeLimitSeconds").value(30));
        postQuestion(dozent, MC).andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("MULTIPLE_CHOICE"));
        postQuestion(dozent, NUMERIC).andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("NUMERIC"))
                .andExpect(jsonPath("$.correctValue").value(32));
        postQuestion(dozent, FREE_TEXT).andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("FREE_TEXT"))
                .andExpect(jsonPath("$.options.length()").value(0));

        mvc.perform(get("/api/questions").session(dozent)).andExpect(jsonPath("$.length()").value(4));
        postQuestion(dozent, "{\"text\":\"ohne Typ\"}").andExpect(status().isBadRequest());
        postQuestion(dozent, "{\"type\":\"SINGLE_CHOICE\",\"text\":\"  \",\"options\":"
                + "[{\"text\":\"A\",\"correct\":true},{\"text\":\"B\",\"correct\":false}]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("/MK50/ Fragen bearbeiten und löschen")
    void bearbeitenUndLoeschen() throws Exception {
        MockHttpSession dozent = newDozent();
        String id = read(createQuestion(dozent, F1), "$.id");

        String changed = F1.replace("Auf welcher OSI-Schicht arbeitet IP?", "Auf welcher Schicht arbeitet IPv6?")
                .replace("\"timeLimitSeconds\":30", "\"timeLimitSeconds\":60");
        mvc.perform(json(put("/api/questions/" + id), changed).session(dozent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Auf welcher Schicht arbeitet IPv6?"))
                .andExpect(jsonPath("$.timeLimitSeconds").value(60));
        mvc.perform(get("/api/questions/" + id).session(dozent))
                .andExpect(jsonPath("$.text").value("Auf welcher Schicht arbeitet IPv6?"));

        // Der Fragetyp lässt sich nachträglich nicht ändern
        mvc.perform(json(put("/api/questions/" + id), MC).session(dozent)).andExpect(status().isBadRequest());

        mvc.perform(delete("/api/questions/" + id).session(dozent)).andExpect(status().isNoContent());
        mvc.perform(get("/api/questions/" + id).session(dozent)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("/MK70/ 2 bis 6 Antwortmöglichkeiten; SC genau eine, MC mindestens eine korrekt")
    void antwortmoeglichkeiten() throws Exception {
        MockHttpSession dozent = newDozent();
        postQuestion(dozent, choice("SINGLE_CHOICE", 1, 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mindestens 2 Antwortmöglichkeiten nötig."));
        postQuestion(dozent, choice("SINGLE_CHOICE", 2, 1)).andExpect(status().isCreated());
        postQuestion(dozent, choice("SINGLE_CHOICE", 6, 1)).andExpect(status().isCreated());
        postQuestion(dozent, choice("SINGLE_CHOICE", 7, 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Höchstens 6 Antwortmöglichkeiten erlaubt."));
        postQuestion(dozent, choice("SINGLE_CHOICE", 3, 0)).andExpect(status().isBadRequest());
        postQuestion(dozent, choice("SINGLE_CHOICE", 3, 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bei Single Choice muss genau eine Antwort korrekt sein."));

        postQuestion(dozent, choice("MULTIPLE_CHOICE", 3, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mindestens eine korrekte Antwort nötig."));
        postQuestion(dozent, choice("MULTIPLE_CHOICE", 3, 1)).andExpect(status().isCreated());
        postQuestion(dozent, choice("MULTIPLE_CHOICE", 6, 6)).andExpect(status().isCreated());
        postQuestion(dozent, choice("MULTIPLE_CHOICE", 7, 1)).andExpect(status().isBadRequest());

        postQuestion(dozent, """
                {"type":"SINGLE_CHOICE","text":"Leere Option",
                 "options":[{"text":"A","correct":true},{"text":"  ","correct":false}]}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("/MK80/ Numerische Fragen: korrekter Wert und nicht negative Toleranz")
    void numerischeFragen() throws Exception {
        MockHttpSession dozent = newDozent();
        postQuestion(dozent, "{\"type\":\"NUMERIC\",\"text\":\"Wert fehlt\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Der korrekte Wert fehlt."));
        postQuestion(dozent, "{\"type\":\"NUMERIC\",\"text\":\"Negativ\",\"correctValue\":10,\"tolerance\":-1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Toleranz darf nicht negativ sein."));
        postQuestion(dozent, "{\"type\":\"NUMERIC\",\"text\":\"Ohne Toleranz\",\"correctValue\":10}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tolerance").value(0));
        postQuestion(dozent, "{\"type\":\"NUMERIC\",\"text\":\"Pi\",\"correctValue\":3.14,\"tolerance\":0.01}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correctValue").value(3.14))
                .andExpect(jsonPath("$.tolerance").value(0.01));
    }

    @Test
    @DisplayName("/MK90/ Freitextfragen haben keine Lösung")
    void freitextOhneLoesung() throws Exception {
        MockHttpSession dozent = newDozent();
        postQuestion(dozent, FREE_TEXT).andExpect(status().isCreated())
                .andExpect(jsonPath("$.options.length()").value(0))
                .andExpect(jsonPath("$.correctValue").doesNotExist());
    }

    @Test
    @DisplayName("/MK100/ Zeitlimit 10 bis 300 Sekunden, Standard 45 Sekunden")
    void zeitlimit() throws Exception {
        MockHttpSession dozent = newDozent();
        postQuestion(dozent, FREE_TEXT).andExpect(jsonPath("$.timeLimitSeconds").value(45));
        postQuestion(dozent, withTime(10)).andExpect(status().isCreated());
        postQuestion(dozent, withTime(300)).andExpect(status().isCreated());
        postQuestion(dozent, withTime(9))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Zeitlimit muss zwischen 10 und 300 Sekunden liegen."));
        postQuestion(dozent, withTime(301)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("/MK110/, /MK120/ Mehrere Tags je Frage, Filter nach Tag")
    void tagsUndFilter() throws Exception {
        MockHttpSession dozent = newDozent();
        createQuestion(dozent, F1.replace("\"tags\":[\"ip\"]", "\"tags\":[\"IP\",\"Schicht 3\"]"));
        createQuestion(dozent, NUMERIC.replace("\"tolerance\":0", "\"tolerance\":0,\"tags\":[\"ip\",\"adressen\"]"));
        createQuestion(dozent, FREE_TEXT.replace("}", ",\"tags\":[\"feedback\"]}"));

        mvc.perform(get("/api/questions").session(dozent)).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/questions").param("tag", "ip").session(dozent))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/questions").param("tag", "IP").session(dozent))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/questions").param("tag", "schicht 3").session(dozent))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tags[0]").value("ip"))
                .andExpect(jsonPath("$[0].tags[1]").value("schicht 3"));
        mvc.perform(get("/api/questions").param("tag", "tcp").session(dozent))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/questions").param("tag", "").session(dozent))
                .andExpect(jsonPath("$.length()").value(3));

        // Schnellstart-Fragen erscheinen nicht im Katalog (/MK200/)
        String sessionId = read(createSession(dozent, "Rechnernetze VL 05"), "$.id");
        quickStartRequest(dozent, sessionId, "{\"type\":\"FREE_TEXT\"}").andExpect(status().isCreated());
        mvc.perform(get("/api/questions").session(dozent)).andExpect(jsonPath("$.length()").value(3));
    }

    /** Choice-Frage mit {@code count} Antwortmöglichkeiten, davon die ersten {@code correct} korrekt. */
    private static String choice(String type, int count, int correct) {
        StringBuilder options = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                options.append(',');
            }
            options.append("{\"text\":\"Antwort ").append(i + 1).append("\",\"correct\":").append(i < correct)
                    .append('}');
        }
        return "{\"type\":\"" + type + "\",\"text\":\"Frage mit " + count + " Antworten\",\"options\":["
                + options + "]}";
    }

    private static String withTime(int seconds) {
        return "{\"type\":\"FREE_TEXT\",\"text\":\"Zeit " + seconds + "\",\"timeLimitSeconds\":" + seconds + "}";
    }
}
