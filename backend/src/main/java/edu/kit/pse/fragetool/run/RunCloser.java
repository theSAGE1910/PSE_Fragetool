package edu.kit.pse.fragetool.run;

import edu.kit.pse.fragetool.common.SessionChangedEvent;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Beendet eine Frage und löst die Bewertung aus.
 * Wird vom Zeitgeber (Zeitablauf, /MK250/) und vom Dozenten (vorzeitig, /MK260/) genutzt.
 */
@Service
public class RunCloser {

    private final QuestionRunRepository runs;
    private final AnswerRepository answers;
    private final ScoringStrategy scoring;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RunCloser(QuestionRunRepository runs, AnswerRepository answers, ScoringStrategy scoring,
            ApplicationEventPublisher events, Clock clock) {
        this.runs = runs;
        this.answers = answers;
        this.scoring = scoring;
        this.events = events;
        this.clock = clock;
    }

    /** Hat keine Wirkung, wenn die Frage schon beendet oder die Session gelöscht ist. */
    @Transactional
    public void close(UUID runId) {
        QuestionRun run = runs.findById(runId).orElse(null);
        if (run == null || !run.isRunning()) {
            return;
        }
        run.closeAndEvaluate(clock.instant(), answers.findWithParticipantByRun(run), scoring);
        events.publishEvent(new SessionChangedEvent(run.getSession().getJoinCode()));
    }
}
