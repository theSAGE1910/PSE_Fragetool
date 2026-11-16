package edu.kit.pse.fragetool.common;

/** Ein Teilnehmer ist einer Session beigetreten. */
public record ParticipantCountChangedEvent(String joinCode, long participantCount) {
}
