package edu.kit.pse.fragetool.question;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.common.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Numerische Frage mit korrektem Wert und absoluter Toleranz (/MK80/). */
@Entity
@DiscriminatorValue("NUMERIC")
public class NumericQuestion extends Question {

    @Column(precision = 30, scale = 10)
    private BigDecimal correctValue;

    @Column(precision = 30, scale = 10)
    private BigDecimal tolerance;

    protected NumericQuestion() {
        // für JPA
    }

    public NumericQuestion(Dozent owner, String text, int timeLimitSeconds, boolean quickStart) {
        super(owner, text, timeLimitSeconds, quickStart);
    }

    @Override
    public QuestionType getType() {
        return QuestionType.NUMERIC;
    }

    @Override
    protected boolean hasSolution() {
        return true;
    }

    @Override
    public void configure(List<OptionSpec> options, BigDecimal newCorrectValue, BigDecimal newTolerance) {
        this.correctValue = newCorrectValue;
        this.tolerance = newTolerance == null ? BigDecimal.ZERO : newTolerance;
    }

    @Override
    protected void validateDetails() {
        if (isQuickStart()) {
            return;
        }
        if (correctValue == null) {
            throw new ValidationException("Der korrekte Wert fehlt.");
        }
        if (tolerance.signum() < 0) {
            throw new ValidationException("Toleranz darf nicht negativ sein.");
        }
    }

    @Override
    public void validateAnswer(AnswerContent answer) {
        if (answer.numericValue() == null) {
            throw new ValidationException("Bitte eine Zahl eingeben.");
        }
    }

    /** Korrekt, wenn |Antwort - Wert| <= Toleranz, die Grenzen eingeschlossen. */
    @Override
    public boolean evaluate(AnswerContent answer) {
        BigDecimal difference = answer.numericValue().subtract(correctValue).abs();
        return difference.compareTo(tolerance) <= 0;
    }

    /** Werte mit Häufigkeiten, aufsteigend sortiert (/MK290/). */
    @Override
    public Distribution distribution(Collection<AnswerContent> answers) {
        Map<BigDecimal, Long> counts = new TreeMap<>();
        for (AnswerContent answer : answers) {
            counts.merge(answer.numericValue().stripTrailingZeros(), 1L, Long::sum);
        }
        List<Distribution.Entry> entries = new ArrayList<>();
        counts.forEach((value, count) -> entries.add(new Distribution.Entry(
                value.toPlainString(), count,
                isGradable() ? evaluate(AnswerContent.ofNumber(value)) : null)));
        return new Distribution(entries);
    }

    @Override
    public String solutionText() {
        if (!isGradable()) {
            return null;
        }
        return tolerance.signum() == 0 ? format(correctValue) : format(correctValue) + " ± " + format(tolerance);
    }

    @Override
    public BigDecimal getCorrectValue() {
        return correctValue;
    }

    @Override
    public BigDecimal getTolerance() {
        return tolerance;
    }
}
