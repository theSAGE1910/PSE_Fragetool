package edu.kit.pse.fragetool.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.session.Participant;
import edu.kit.pse.fragetool.session.Session;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Prüft /MK320/, /MK330/ und /MK335/. */
class ScoreboardTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
    private final Session session = new Session(new Dozent("d@test.de", "hash", NOW), "VL", "CODE23", NOW);

    private Participant participant(String name, int points) {
        Participant p = new Participant(session, name, "token-" + name, NOW);
        p.addPoints(points);
        return p;
    }

    @Test
    void sortiertNachPunktenUndBeiGleichstandNachNamen() {
        Scoreboard board = Scoreboard.of(List.of(participant("ZZZ", 2), participant("BBB", 3), participant("CCC", 2)));
        assertEquals(List.of(new ScoreboardEntry(1, "BBB", 3), new ScoreboardEntry(2, "CCC", 2),
                new ScoreboardEntry(2, "ZZZ", 2)), board.top(10));
    }

    @Test
    void ueberspringtRaengeNachGleichstand() {
        Scoreboard board = Scoreboard.of(List.of(participant("BBB", 5), participant("CCC", 5), participant("DDD", 1)));
        assertEquals(List.of(1, 1, 3), board.top(10).stream().map(ScoreboardEntry::rank).toList());
        assertEquals(3, board.entryOf("DDD").orElseThrow().rank());
    }

    @Test
    void zeigtHoechstensZehnEintraege() {
        List<Participant> twelve = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            twelve.add(participant("P" + (char) ('B' + i) + "X", i));
        }
        Scoreboard board = Scoreboard.of(twelve);
        assertEquals(10, board.top(Scoreboard.PRESENTATION_SIZE).size());
        assertEquals(11, board.top(Scoreboard.PRESENTATION_SIZE).get(0).points());
        assertEquals(12, board.size());
    }
}
