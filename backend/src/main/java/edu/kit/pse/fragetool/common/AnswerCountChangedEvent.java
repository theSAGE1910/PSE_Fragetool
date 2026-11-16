package edu.kit.pse.fragetool.common;

/** Für die laufende Frage einer Session wurde eine Antwort gespeichert (/MK300/). */
public record AnswerCountChangedEvent(String joinCode, long answerCount) {
}
