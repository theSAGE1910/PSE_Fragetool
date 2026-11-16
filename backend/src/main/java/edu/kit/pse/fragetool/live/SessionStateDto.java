package edu.kit.pse.fragetool.live;

import edu.kit.pse.fragetool.question.Distribution;
import edu.kit.pse.fragetool.question.QuestionType;
import edu.kit.pse.fragetool.run.RunStatus;
import edu.kit.pse.fragetool.scoreboard.ScoreboardEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Öffentlicher Zustand einer Session für Präsentations- und Teilnehmeransicht.
 * Enthält die Lösung erst nach Fragenende (/MK270/).
 *
 * @param serverNow  Serverzeit; die Ansichten berechnen daraus nur die Anzeige der Restzeit (/MK430/)
 * @param scoreboard die besten zehn Teilnehmer, leer solange das Scoreboard nicht eingeblendet ist
 * @param run        die zuletzt gestartete Frage oder {@code null}
 */
public record SessionStateDto(
        String sessionName,
        String joinCode,
        Instant serverNow,
        long participantCount,
        boolean scoreboardVisible,
        List<ScoreboardEntry> scoreboard,
        RunDto run) {

    public record RunDto(
            UUID id,
            RunStatus status,
            Instant startedAt,
            Instant endsAt,
            long answerCount,
            boolean freeTextReleased,
            QuestionView question,
            ResultView result) {
    }

    /** Die Frage ohne Lösung. */
    public record QuestionView(
            QuestionType type,
            String text,
            boolean quickStart,
            boolean gradable,
            String imageUrl,
            List<OptionView> options) {
    }

    public record OptionView(UUID id, String text) {
    }

    /** Auswertung nach Fragenende; {@code solution} ist {@code null} bei nicht bewertbaren Fragen. */
    public record ResultView(List<Distribution.Entry> distribution, String solution) {
    }
}
