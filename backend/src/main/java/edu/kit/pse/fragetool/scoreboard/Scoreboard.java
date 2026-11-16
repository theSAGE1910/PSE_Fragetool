package edu.kit.pse.fragetool.scoreboard;

import edu.kit.pse.fragetool.session.Participant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Rangfolge der Teilnehmer einer Session (/MK320/ bis /MK335/).
 *
 * <p>Sortiert wird nach Punkten absteigend, bei Gleichstand nach Anzeigenamen.
 * Teilnehmer mit gleicher Punktzahl erhalten denselben Rang; der folgende Rang
 * wird übersprungen (1, 1, 3).
 */
public final class Scoreboard {

    public static final int PRESENTATION_SIZE = 10;

    private final List<ScoreboardEntry> entries;

    private Scoreboard(List<ScoreboardEntry> entries) {
        this.entries = entries;
    }

    public static Scoreboard of(Collection<Participant> participants) {
        List<Participant> sorted = new ArrayList<>(participants);
        sorted.sort(Comparator.comparingInt(Participant::getPoints).reversed()
                .thenComparing(Participant::getDisplayName));
        List<ScoreboardEntry> entries = new ArrayList<>();
        int rank = 0;
        int previousPoints = Integer.MIN_VALUE;
        for (int i = 0; i < sorted.size(); i++) {
            Participant participant = sorted.get(i);
            if (participant.getPoints() != previousPoints) {
                rank = i + 1;
                previousPoints = participant.getPoints();
            }
            entries.add(new ScoreboardEntry(rank, participant.getDisplayName(), participant.getPoints()));
        }
        return new Scoreboard(List.copyOf(entries));
    }

    /** Die ersten {@code n} Einträge, höchstens so viele wie vorhanden. */
    public List<ScoreboardEntry> top(int n) {
        return entries.subList(0, Math.min(n, entries.size()));
    }

    public Optional<ScoreboardEntry> entryOf(String displayName) {
        return entries.stream().filter(e -> e.displayName().equals(displayName)).findFirst();
    }

    public int size() {
        return entries.size();
    }
}
