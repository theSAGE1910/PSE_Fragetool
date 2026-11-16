package edu.kit.pse.fragetool.run;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Eine abgegebene Antwort. Je nach Fragetyp ist genau eines der Felder gefüllt. */
public record AnswerRequest(List<UUID> optionIds, BigDecimal numericValue, String text) {
}
