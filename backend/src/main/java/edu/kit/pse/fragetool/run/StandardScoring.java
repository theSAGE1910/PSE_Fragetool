package edu.kit.pse.fragetool.run;

/** Ein Punkt pro korrekter Antwort (/MK310/). */
public class StandardScoring implements ScoringStrategy {

    private static final int POINTS_PER_CORRECT_ANSWER = 1;

    @Override
    public int pointsFor(QuestionRun run, Answer correctAnswer) {
        return POINTS_PER_CORRECT_ANSWER;
    }
}
