package edu.kit.pse.fragetool.question;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.common.ValidationException;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/** Freitextfrage: keine Lösung, keine Bewertung (/MK90/). */
@Entity
@DiscriminatorValue("FREE_TEXT")
public class FreeTextQuestion extends Question {

    public static final int MAX_ANSWER_LENGTH = 500;

    protected FreeTextQuestion() {
        // für JPA
    }

    public FreeTextQuestion(Dozent owner, String text, int timeLimitSeconds, boolean quickStart) {
        super(owner, text, timeLimitSeconds, quickStart);
    }

    @Override
    public QuestionType getType() {
        return QuestionType.FREE_TEXT;
    }

    @Override
    protected boolean hasSolution() {
        return false;
    }

    @Override
    public void configure(List<OptionSpec> options, BigDecimal correctValue, BigDecimal tolerance) {
        // Freitextfragen haben keine weiteren Angaben.
    }

    @Override
    protected void validateDetails() {
        // keine weiteren Regeln
    }

    @Override
    public void validateAnswer(AnswerContent answer) {
        if (answer.text() == null || answer.text().isBlank()) {
            throw new ValidationException("Bitte eine Antwort eingeben.");
        }
        if (answer.text().length() > MAX_ANSWER_LENGTH) {
            throw new ValidationException("Die Antwort ist zu lang (höchstens 500 Zeichen).");
        }
    }

    @Override
    public boolean evaluate(AnswerContent answer) {
        return false;
    }

    /** Liste aller Antworten in der Reihenfolge der Abgabe (/MK290/). */
    @Override
    public Distribution distribution(Collection<AnswerContent> answers) {
        return new Distribution(answers.stream()
                .map(a -> new Distribution.Entry(a.text(), 1, null))
                .toList());
    }

    @Override
    public String solutionText() {
        return null;
    }
}
