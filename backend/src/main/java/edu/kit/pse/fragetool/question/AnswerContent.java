package edu.kit.pse.fragetool.question;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * Inhalt einer abgegebenen Antwort, unabhängig vom Fragetyp.
 * Je nach Fragetyp ist genau eines der Felder gefüllt.
 */
public record AnswerContent(Set<UUID> optionIds, BigDecimal numericValue, String text) {

    public AnswerContent {
        optionIds = optionIds == null ? Set.of() : Set.copyOf(optionIds);
    }

    public static AnswerContent ofOptions(Set<UUID> optionIds) {
        return new AnswerContent(optionIds, null, null);
    }

    public static AnswerContent ofNumber(BigDecimal value) {
        return new AnswerContent(null, value, null);
    }

    public static AnswerContent ofText(String text) {
        return new AnswerContent(null, null, text);
    }
}
