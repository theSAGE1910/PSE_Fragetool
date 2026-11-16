package edu.kit.pse.fragetool.question;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.common.ValidationException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Gemeinsame Basis für Single und Multiple Choice: 2 bis 6 Antwortmöglichkeiten (/MK70/). */
@Entity
public abstract class ChoiceQuestion extends Question {

    public static final int MIN_OPTIONS = 2;
    public static final int MAX_OPTIONS = 6;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortIndex ASC")
    private List<AnswerOption> options = new ArrayList<>();

    protected ChoiceQuestion() {
        // für JPA
    }

    protected ChoiceQuestion(Dozent owner, String text, int timeLimitSeconds, boolean quickStart) {
        super(owner, text, timeLimitSeconds, quickStart);
    }

    /** Regel des Untertyps für die Anzahl korrekter Antwortmöglichkeiten. */
    protected abstract void validateCorrectCount(long correctCount);

    @Override
    protected boolean hasSolution() {
        return true;
    }

    @Override
    public void configure(List<OptionSpec> specs, BigDecimal correctValue, BigDecimal tolerance) {
        options.clear();
        if (specs == null) {
            return;
        }
        int index = 0;
        for (OptionSpec spec : specs) {
            String optionText = spec.text() == null ? "" : spec.text().strip();
            options.add(new AnswerOption(this, optionText, spec.correct(), index++));
        }
    }

    @Override
    protected void validateDetails() {
        if (options.size() < MIN_OPTIONS) {
            throw new ValidationException("Mindestens 2 Antwortmöglichkeiten nötig.");
        }
        if (options.size() > MAX_OPTIONS) {
            throw new ValidationException("Höchstens 6 Antwortmöglichkeiten erlaubt.");
        }
        for (AnswerOption option : options) {
            if (option.getText().isBlank()) {
                throw new ValidationException("Antwortmöglichkeiten dürfen nicht leer sein.");
            }
            if (option.getText().length() > AnswerOption.MAX_TEXT_LENGTH) {
                throw new ValidationException("Eine Antwortmöglichkeit ist zu lang.");
            }
        }
        if (!isQuickStart()) {
            validateCorrectCount(correctOptionIds().size());
        }
    }

    @Override
    public void validateAnswer(AnswerContent answer) {
        Set<UUID> known = options.stream().map(AnswerOption::getId).collect(Collectors.toSet());
        if (answer.optionIds().isEmpty()) {
            throw new ValidationException("Bitte eine Antwort auswählen.");
        }
        if (!known.containsAll(answer.optionIds())) {
            throw new ValidationException("Die Antwort gehört nicht zu dieser Frage.");
        }
    }

    @Override
    public boolean evaluate(AnswerContent answer) {
        return answer.optionIds().equals(correctOptionIds());
    }

    /** Bei Multiple Choice wird jede gewählte Antwortmöglichkeit einzeln gezählt (/MK290/). */
    @Override
    public Distribution distribution(Collection<AnswerContent> answers) {
        List<Distribution.Entry> entries = new ArrayList<>();
        for (AnswerOption option : options) {
            long count = answers.stream().filter(a -> a.optionIds().contains(option.getId())).count();
            entries.add(new Distribution.Entry(option.getText(), count, isGradable() ? option.isCorrect() : null));
        }
        return new Distribution(entries);
    }

    @Override
    public String solutionText() {
        if (!isGradable()) {
            return null;
        }
        return options.stream().filter(AnswerOption::isCorrect).map(AnswerOption::getText)
                .collect(Collectors.joining(", "));
    }

    @Override
    public List<AnswerOption> getOptions() {
        return List.copyOf(options);
    }

    public Set<UUID> correctOptionIds() {
        return options.stream().filter(AnswerOption::isCorrect).map(AnswerOption::getId)
                .collect(Collectors.toSet());
    }
}
