package edu.kit.pse.fragetool.scoreboard;

/** Eine Zeile des Scoreboards. */
public record ScoreboardEntry(int rank, String displayName, int points) {
}
