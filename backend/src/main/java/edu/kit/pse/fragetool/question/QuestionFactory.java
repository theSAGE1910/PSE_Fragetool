package edu.kit.pse.fragetool.question;

import edu.kit.pse.fragetool.auth.Dozent;

/** Erzeugt die passende Unterklasse zu einem Fragetyp. Einzige Stelle mit einem switch über den Typ. */
public final class QuestionFactory {

    private QuestionFactory() {
    }

    public static Question create(QuestionType type, Dozent owner, String text, int timeLimitSeconds,
            boolean quickStart) {
        return switch (type) {
            case SINGLE_CHOICE -> new SingleChoiceQuestion(owner, text, timeLimitSeconds, quickStart);
            case MULTIPLE_CHOICE -> new MultipleChoiceQuestion(owner, text, timeLimitSeconds, quickStart);
            case NUMERIC -> new NumericQuestion(owner, text, timeLimitSeconds, quickStart);
            case FREE_TEXT -> new FreeTextQuestion(owner, text, timeLimitSeconds, quickStart);
        };
    }
}
