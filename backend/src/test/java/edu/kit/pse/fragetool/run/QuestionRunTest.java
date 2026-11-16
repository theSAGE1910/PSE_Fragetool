package edu.kit.pse.fragetool.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.common.ConflictException;
import edu.kit.pse.fragetool.question.AnswerContent;
import edu.kit.pse.fragetool.question.OptionSpec;
import edu.kit.pse.fragetool.question.Question;
import edu.kit.pse.fragetool.question.QuestionFactory;
import edu.kit.pse.fragetool.question.QuestionType;
import edu.kit.pse.fragetool.session.Participant;
import edu.kit.pse.fragetool.session.Session;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Prüft /MK250/, /MK255/ und /MK310/ ohne Datenbank und ohne echte Wartezeit. */
class QuestionRunTest {

    private static final Instant START = Instant.parse("2026-11-01T10:00:00Z");

    private final Dozent dozent = new Dozent("dozent@test.de", "hash", START);
    private final Session session = new Session(dozent, "Rechnernetze VL 05", "RN5K2P", START);
    private final Participant alice = new Participant(session, "K7X", "token-a", START);
    private final Participant bob = new Participant(session, "B4D", "token-b", START);

    private Question singleChoice(boolean quickStart) {
        Question q = QuestionFactory.create(QuestionType.SINGLE_CHOICE, dozent, "IP?", 30, quickStart);
        q.configure(List.of(new OptionSpec("Schicht 2", false), new OptionSpec("Schicht 3", !quickStart)), null, null);
        return q;
    }

    private Answer answer(QuestionRun run, Participant participant, int optionIndex) {
        return new Answer(run, participant, AnswerContent.ofOptions(
                Set.of(run.getQuestion().getOptions().get(optionIndex).getId())), START.plusSeconds(5));
    }

    @Test
    void nimmtAntwortenNurVorAblaufAn() {
        QuestionRun run = new QuestionRun(session, singleChoice(false), START);
        assertEquals(START.plusSeconds(30), run.getEndsAt());
        run.ensureAccepting(START.plusSeconds(29).plusMillis(999));
        assertThrows(ConflictException.class, () -> run.ensureAccepting(START.plusSeconds(30)));
        assertThrows(ConflictException.class, () -> run.ensureAccepting(START.plusSeconds(31)));
    }

    @Test
    void lehntAntwortenNachVorzeitigemEndeAb() {
        QuestionRun run = new QuestionRun(session, singleChoice(false), START);
        run.closeAndEvaluate(START.plusSeconds(10), List.of(), new StandardScoring());
        assertFalse(run.isRunning());
        assertThrows(ConflictException.class, () -> run.ensureAccepting(START.plusSeconds(11)));
    }

    @Test
    void vergibtEinenPunktFuerKorrekteAntworten() {
        QuestionRun run = new QuestionRun(session, singleChoice(false), START);
        Answer richtig = answer(run, alice, 1);
        Answer falsch = answer(run, bob, 0);
        run.closeAndEvaluate(START.plusSeconds(30), List.of(richtig, falsch), new StandardScoring());
        assertEquals(1, alice.getPoints());
        assertEquals(0, bob.getPoints());
        assertEquals(Boolean.TRUE, richtig.getCorrect());
        assertEquals(Boolean.FALSE, falsch.getCorrect());
    }

    @Test
    void zweitesSchliessenVergibtKeinePunkteDoppelt() {
        QuestionRun run = new QuestionRun(session, singleChoice(false), START);
        List<Answer> answers = List.of(answer(run, alice, 1));
        run.closeAndEvaluate(START.plusSeconds(30), answers, new StandardScoring());
        run.closeAndEvaluate(START.plusSeconds(31), answers, new StandardScoring());
        assertEquals(1, alice.getPoints());
        assertEquals(START.plusSeconds(30), run.getClosedAt());
    }

    @Test
    void schnellstartFragenErgebenKeinePunkte() {
        QuestionRun run = new QuestionRun(session, singleChoice(true), START);
        Answer a = answer(run, alice, 1);
        run.closeAndEvaluate(START.plusSeconds(30), List.of(a), new StandardScoring());
        assertEquals(0, alice.getPoints());
        assertNull(a.getCorrect());
    }
}
