package edu.kit.pse.fragetool.live;

import java.util.UUID;

/**
 * Eigene Daten eines Teilnehmers (/MK280/, /MK330/).
 *
 * @param lastRunId die zuletzt gestartete Frage der Session oder {@code null}
 * @param answered  ob der Teilnehmer diese Frage beantwortet hat
 * @param correct   ob die Antwort korrekt war; {@code null}, solange die Frage läuft oder wenn sie nicht bewertet wird
 */
public record MeDto(
        String displayName,
        int points,
        int rank,
        int participantCount,
        String sessionName,
        String joinCode,
        UUID lastRunId,
        boolean answered,
        Boolean correct) {
}
