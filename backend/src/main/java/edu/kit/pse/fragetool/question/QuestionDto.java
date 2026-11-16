package edu.kit.pse.fragetool.question;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Eine Frage, wie der Dozent sie im Katalog und im Editor sieht (mit Lösung). */
public record QuestionDto(
        UUID id,
        QuestionType type,
        String text,
        int timeLimitSeconds,
        List<String> tags,
        List<OptionDto> options,
        BigDecimal correctValue,
        BigDecimal tolerance,
        String imageUrl) {

    public record OptionDto(UUID id, String text, boolean correct) {
    }

    public static QuestionDto from(Question question) {
        return new QuestionDto(
                question.getId(),
                question.getType(),
                question.getText(),
                question.getTimeLimitSeconds(),
                question.getTags().stream().sorted().toList(),
                question.getOptions().stream()
                        .map(o -> new OptionDto(o.getId(), o.getText(), o.isCorrect())).toList(),
                plain(question.getCorrectValue()),
                plain(question.getTolerance()),
                question.getImageUrl());
    }

    /** Entfernt überflüssige Nachkommastellen, ohne in die Exponentialschreibweise zu wechseln. */
    private static BigDecimal plain(BigDecimal value) {
        return value == null ? null : new BigDecimal(value.stripTrailingZeros().toPlainString());
    }
}
