package edu.kit.pse.fragetool.common;

/** Der Zustand einer Session hat sich geändert (Frage gestartet, beendet, Scoreboard ...). */
public record SessionChangedEvent(String joinCode) {
}
