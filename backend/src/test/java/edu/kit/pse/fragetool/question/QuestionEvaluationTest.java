package edu.kit.pse.fragetool.question;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.common.ValidationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Prüft die Regeln aus /MK70/, /MK80/, /MK90/, /MK100/, /MK290/ und /MK310/. */
class QuestionEvaluationTest {

    private final Dozent dozent = new Dozent("dozent@test.de", "hash", Instant.parse("2026-11-01T10:00:00Z"));

    private Question question(QuestionType type, List<OptionSpec> options, String value, String tolerance) {
        Question q = QuestionFactory.create(type, dozent, "Frage?", 30, false);
        q.configure(options, value == null ? null : new BigDecimal(value),
                tolerance == null ? null : new BigDecimal(tolerance));
        return q;
    }

    private Question multipleChoiceAC() {
        return question(QuestionType.MULTIPLE_CHOICE, List.of(new OptionSpec("A", true), new OptionSpec("B", false),
                new OptionSpec("C", true), new OptionSpec("D", false)), null, null);
    }

    private static UUID id(Question q, int index) {
        return q.getOptions().get(index).getId();
    }

    @Test
    void singleChoiceIstNurMitDerKorrektenAntwortRichtig() {
        Question q = question(QuestionType.SINGLE_CHOICE, List.of(new OptionSpec("Schicht 2", false),
                new OptionSpec("Schicht 3", true), new OptionSpec("Schicht 4", false)), null, null);
        q.validate();
        assertTrue(q.evaluate(AnswerContent.ofOptions(Set.of(id(q, 1)))));
        assertFalse(q.evaluate(AnswerContent.ofOptions(Set.of(id(q, 0)))));
        assertEquals("Schicht 3", q.solutionText());
    }

    @Test
    void multipleChoiceIstNurMitExakterMengeRichtig() {
        Question q = multipleChoiceAC();
        assertTrue(q.evaluate(AnswerContent.ofOptions(Set.of(id(q, 0), id(q, 2)))));
        assertFalse(q.evaluate(AnswerContent.ofOptions(Set.of(id(q, 0)))));
        assertFalse(q.evaluate(AnswerContent.ofOptions(Set.of(id(q, 0), id(q, 1), id(q, 2)))));
    }

    @Test
    void multipleChoiceZaehltJedeGewaehlteAntwortEinzeln() {
        Question q = multipleChoiceAC();
        Distribution d = q.distribution(List.of(
                AnswerContent.ofOptions(Set.of(id(q, 0), id(q, 2))),
                AnswerContent.ofOptions(Set.of(id(q, 0))),
                AnswerContent.ofOptions(Set.of(id(q, 0), id(q, 1), id(q, 2)))));
        assertEquals(List.of(3L, 1L, 2L, 0L), d.entries().stream().map(Distribution.Entry::count).toList());
        assertEquals(Boolean.TRUE, d.entries().get(0).correct());
        assertEquals(Boolean.FALSE, d.entries().get(1).correct());
    }

    @Test
    void numerischeToleranzSchliesstDieGrenzenEin() {
        Question q = question(QuestionType.NUMERIC, null, "100", "5");
        q.validate();
        assertTrue(q.evaluate(AnswerContent.ofNumber(new BigDecimal("95"))));
        assertTrue(q.evaluate(AnswerContent.ofNumber(new BigDecimal("105"))));
        assertTrue(q.evaluate(AnswerContent.ofNumber(new BigDecimal("105.0"))));
        assertFalse(q.evaluate(AnswerContent.ofNumber(new BigDecimal("94.99"))));
        assertFalse(q.evaluate(AnswerContent.ofNumber(new BigDecimal("105.01"))));
        assertEquals("100 ± 5", q.solutionText());
    }

    @Test
    void numerischeVerteilungFasstGleicheWerteZusammen() {
        Question q = question(QuestionType.NUMERIC, null, "100", "5");
        Distribution d = q.distribution(List.of(AnswerContent.ofNumber(new BigDecimal("105")),
                AnswerContent.ofNumber(new BigDecimal("106")), AnswerContent.ofNumber(new BigDecimal("95")),
                AnswerContent.ofNumber(new BigDecimal("105.00"))));
        assertEquals(List.of("95", "105", "106"), d.entries().stream().map(Distribution.Entry::label).toList());
        assertEquals(List.of(1L, 2L, 1L), d.entries().stream().map(Distribution.Entry::count).toList());
        assertEquals(Boolean.FALSE, d.entries().get(2).correct());
    }

    @Test
    void negativeToleranzWirdAbgelehnt() {
        Question q = question(QuestionType.NUMERIC, null, "32", "-1");
        assertThrows(ValidationException.class, q::validate);
    }

    @Test
    void freitextIstNichtBewertbar() {
        Question q = question(QuestionType.FREE_TEXT, null, null, null);
        q.validate();
        assertFalse(q.isGradable());
        assertNull(q.solutionText());
        Distribution d = q.distribution(List.of(AnswerContent.ofText("Routing"), AnswerContent.ofText("Subnetting")));
        assertEquals(2, d.entries().size());
        assertNull(d.entries().get(0).correct());
    }

    @Test
    void ungueltigeChoiceFragenWerdenAbgelehnt() {
        Question eineAntwort = question(QuestionType.SINGLE_CHOICE, List.of(new OptionSpec("A", true)), null, null);
        assertThrows(ValidationException.class, eineAntwort::validate);

        Question zweiKorrekte = question(QuestionType.SINGLE_CHOICE,
                List.of(new OptionSpec("A", true), new OptionSpec("B", true)), null, null);
        assertThrows(ValidationException.class, zweiKorrekte::validate);

        Question keineKorrekte = question(QuestionType.MULTIPLE_CHOICE,
                List.of(new OptionSpec("A", false), new OptionSpec("B", false)), null, null);
        assertThrows(ValidationException.class, keineKorrekte::validate);

        Question siebenAntworten = question(QuestionType.MULTIPLE_CHOICE, List.of(new OptionSpec("A", true),
                new OptionSpec("B", false), new OptionSpec("C", false), new OptionSpec("D", false),
                new OptionSpec("E", false), new OptionSpec("F", false), new OptionSpec("G", false)), null, null);
        assertThrows(ValidationException.class, siebenAntworten::validate);
    }

    @Test
    void zeitlimitMussZwischen10Und300SekundenLiegen() {
        for (int seconds : new int[] {9, 301}) {
            Question q = QuestionFactory.create(QuestionType.FREE_TEXT, dozent, "Frage?", seconds, false);
            assertThrows(ValidationException.class, q::validate);
        }
        for (int seconds : new int[] {10, 300}) {
            QuestionFactory.create(QuestionType.FREE_TEXT, dozent, "Frage?", seconds, false).validate();
        }
    }

    @Test
    void schnellstartFragenBrauchenKeineLoesungUndSindNichtBewertbar() {
        Question q = QuestionFactory.create(QuestionType.SINGLE_CHOICE, dozent, "Schnellstart", 20, true);
        q.configure(List.of(new OptionSpec("A", false), new OptionSpec("B", false)), null, null);
        q.validate();
        assertFalse(q.isGradable());
        assertNull(q.solutionText());
        assertNull(q.distribution(List.of()).entries().get(0).correct());
    }

    @Test
    void antwortenMuessenZurFragePassen() {
        Question q = multipleChoiceAC();
        assertThrows(ValidationException.class, () -> q.validateAnswer(AnswerContent.ofOptions(Set.of())));
        assertThrows(ValidationException.class,
                () -> q.validateAnswer(AnswerContent.ofOptions(Set.of(UUID.randomUUID()))));
        q.validateAnswer(AnswerContent.ofOptions(Set.of(id(q, 0), id(q, 1))));
    }
}
