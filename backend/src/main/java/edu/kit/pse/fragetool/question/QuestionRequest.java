package edu.kit.pse.fragetool.question;

import java.math.BigDecimal;
import java.util.List;

/** Eingabe beim Erstellen und Bearbeiten einer Frage. */
public record QuestionRequest(
        QuestionType type,
        String text,
        Integer timeLimitSeconds,
        List<String> tags,
        List<OptionSpec> options,
        BigDecimal correctValue,
        BigDecimal tolerance) {
}
