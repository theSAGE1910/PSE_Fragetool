package edu.kit.pse.fragetool.question;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.common.ValidationException;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Single Choice: genau eine Antwortmöglichkeit ist korrekt (/MK70/). */
@Entity
@DiscriminatorValue("SINGLE_CHOICE")
public class SingleChoiceQuestion extends ChoiceQuestion {

    protected SingleChoiceQuestion() {
        // für JPA
    }

    public SingleChoiceQuestion(Dozent owner, String text, int timeLimitSeconds, boolean quickStart) {
        super(owner, text, timeLimitSeconds, quickStart);
    }

    @Override
    public QuestionType getType() {
        return QuestionType.SINGLE_CHOICE;
    }

    @Override
    protected void validateCorrectCount(long correctCount) {
        if (correctCount != 1) {
            throw new ValidationException("Bei Single Choice muss genau eine Antwort korrekt sein.");
        }
    }

    @Override
    public void validateAnswer(AnswerContent answer) {
        super.validateAnswer(answer);
        if (answer.optionIds().size() != 1) {
            throw new ValidationException("Bei Single Choice ist genau eine Antwort erlaubt.");
        }
    }
}
