package edu.kit.pse.fragetool.run;

/** Legt fest, wie viele Punkte eine korrekte Antwort ergibt. Austauschbar, z. B. für einen Zeitbonus (/WK100/). */
public interface ScoringStrategy {

    int pointsFor(QuestionRun run, Answer correctAnswer);
}
