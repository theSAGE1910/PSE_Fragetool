package edu.kit.pse.fragetool.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.pse.fragetool.run.Answer;
import edu.kit.pse.fragetool.support.ApiTestSupport;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Attribute;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * /MK360/: Über Teilnehmer wird nur gespeichert, was für Teilnahme, Wiedererkennung,
 * Antworten und Punkte nötig ist (Prüfverfahren /P90/).
 * Geprüft werden das JPA-Modell und das Datenbankschema für den Betrieb (Flyway).
 */
class DatensparsamkeitTest extends ApiTestSupport {

    private static final Set<String> PARTICIPANT_FIELDS =
            Set.of("id", "session", "displayName", "token", "points", "joinedAt");
    private static final Set<String> ANSWER_FIELDS = Set.of("id", "run", "participant", "selectedOptionIds",
            "numericValue", "text", "correct", "submittedAt");
    private static final Set<String> PARTICIPANT_COLUMNS =
            Set.of("id", "session_id", "display_name", "token", "points", "joined_at");
    private static final Set<String> ANSWER_COLUMNS = Set.of("id", "run_id", "participant_id", "numeric_value",
            "text", "correct", "submitted_at");

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("/MK360/ Teilnehmer und Antworten enthalten nur die nötigen Felder")
    void entitiesEnthaltenNurNoetigeFelder() {
        assertEquals(PARTICIPANT_FIELDS, attributes(Participant.class));
        assertEquals(ANSWER_FIELDS, attributes(Answer.class));
    }

    @Test
    @DisplayName("/MK360/ Das Betriebsschema (Flyway) speichert keine weiteren Teilnehmerdaten")
    void betriebsschemaEnthaeltNurNoetigeSpalten() throws IOException {
        String sql = migration();
        assertEquals(PARTICIPANT_COLUMNS, columns(sql, "participant"));
        assertEquals(ANSWER_COLUMNS, columns(sql, "answer"));
        String lower = sql.toLowerCase(Locale.ROOT);
        for (String forbidden : new String[] {"ip_address", "user_agent", "real_name", "matrikel"}) {
            assertTrue(!lower.contains(forbidden), "Unerwartete Spalte " + forbidden);
        }
    }

    private Set<String> attributes(Class<?> entity) {
        return entityManagerFactory.getMetamodel().entity(entity).getAttributes().stream()
                .map(Attribute::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static String migration() throws IOException {
        try (InputStream in = DatensparsamkeitTest.class.getResourceAsStream("/db/migration/V1__init.sql")) {
            if (in == null) {
                throw new IllegalStateException("V1__init.sql nicht gefunden");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Spaltennamen einer Tabelle aus dem CREATE-TABLE-Befehl (ohne CONSTRAINT-Zeilen). */
    private static Set<String> columns(String sql, String table) {
        Matcher matcher = Pattern.compile("CREATE TABLE " + table + " \\((.*?)\\n\\);", Pattern.DOTALL)
                .matcher(sql);
        assertTrue(matcher.find(), "Tabelle " + table + " fehlt");
        return Arrays.stream(matcher.group(1).split("\\n"))
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("--") && !line.startsWith("CONSTRAINT")
                        && !line.startsWith("PRIMARY KEY") && !line.startsWith("UNIQUE"))
                .map(line -> line.split("\\s+")[0].toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
