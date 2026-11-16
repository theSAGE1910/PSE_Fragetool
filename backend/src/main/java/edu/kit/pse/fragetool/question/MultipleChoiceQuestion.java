package edu.kit.pse.fragetool.question;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.common.ValidationException;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Multiple Choice: mindestens eine Antwortmöglichkeit ist korrekt (/MK70/).
 * Eine Antwort ist nur korrekt, wenn die gewählte Menge exakt stimmt (/MK310/).
 */
@Entity
@DiscriminatorValue("MULTIPLE_CHOICE")
public class MultipleChoiceQuestion extends ChoiceQuestion {

    protected MultipleChoiceQuestion() {
        // für JPA
    }

    public MultipleChoiceQuestion(Dozent owner, String text, int timeLimitSeconds, boolean quickStart) {
        super(owner, text, timeLimitSeconds, quickStart);
    }

    @Override
    public QuestionType getType() {
        return QuestionType.MULTIPLE_CHOICE;
    }

    @Override
    protected void validateCorrectCount(long correctCount) {
        if (correctCount < 1) {
            throw new ValidationException("Mindestens eine korrekte Antwort nötig.");
        }
    }
}
