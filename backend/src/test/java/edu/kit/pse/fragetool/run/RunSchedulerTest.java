package edu.kit.pse.fragetool.run;

import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.question.OptionSpec;
import edu.kit.pse.fragetool.question.Question;
import edu.kit.pse.fragetool.question.QuestionFactory;
import edu.kit.pse.fragetool.question.QuestionType;
import edu.kit.pse.fragetool.session.Session;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Zeitgeber, der Fragen automatisch beendet: /MK250/. Arbeitet mit echter Zeit (wenige 100 ms). */
class RunSchedulerTest {

    private final RunCloser closer = mock(RunCloser.class);
    private final QuestionRunRepository runs = mock(QuestionRunRepository.class);
    private final RunScheduler scheduler = new RunScheduler(closer, runs);

    @AfterEach
    void stop() {
        scheduler.shutdown();
    }

    @Test
    @DisplayName("/MK250/ Eine Frage wird zu ihrem Endzeitpunkt automatisch beendet")
    void beendetZumEndzeitpunkt() {
        UUID runId = UUID.randomUUID();
        scheduler.schedule(runId, Instant.now().plusMillis(300));
        verify(closer, after(100).never()).close(runId);
        verify(closer, timeout(3000)).close(runId);
    }

    @Test
    @DisplayName("/MK260/ Nach vorzeitigem Beenden wird der Zeitgeber abgebrochen")
    void abbrechen() {
        UUID runId = UUID.randomUUID();
        scheduler.schedule(runId, Instant.now().plusMillis(300));
        scheduler.cancel(runId);
        scheduler.cancel(UUID.randomUUID()); // unbekannte ID: keine Wirkung
        verify(closer, after(800).never()).close(runId);
    }

    @Test
    @DisplayName("/MK250/ Nach einem Neustart werden laufende Fragen erneut eingeplant")
    void neustartPlantLaufendeFragenEin() {
        Instant start = Instant.now().minusSeconds(60);
        Dozent dozent = new Dozent("d@test.de", "hash", start);
        Session session = new Session(dozent, "VL", "BCDFGH", start);
        Question question = QuestionFactory.create(QuestionType.SINGLE_CHOICE, dozent, "IP?", 30, false);
        question.configure(List.of(new OptionSpec("A", true), new OptionSpec("B", false)), null, null);
        QuestionRun overdue = new QuestionRun(session, question, start);
        when(runs.findByStatus(RunStatus.RUNNING)).thenReturn(List.of(overdue));

        scheduler.run(null);
        verify(closer, timeout(3000)).close(overdue.getId());
    }

    @Test
    @DisplayName("Ein Fehler beim Beenden stoppt den Zeitgeber nicht")
    void fehlerStopptZeitgeberNicht() {
        UUID failing = UUID.randomUUID();
        UUID next = UUID.randomUUID();
        doThrow(new IllegalStateException("Datenbank nicht erreichbar")).when(closer).close(failing);
        scheduler.schedule(failing, Instant.now());
        verify(closer, timeout(3000)).close(failing);
        scheduler.schedule(next, Instant.now());
        verify(closer, timeout(3000)).close(next);
        verify(closer, never()).close(UUID.randomUUID());
    }
}
