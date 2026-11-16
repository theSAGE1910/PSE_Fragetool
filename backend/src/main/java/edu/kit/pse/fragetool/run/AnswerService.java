package edu.kit.pse.fragetool.run;

import edu.kit.pse.fragetool.common.AnswerCountChangedEvent;
import edu.kit.pse.fragetool.common.ConflictException;
import edu.kit.pse.fragetool.common.ForbiddenException;
import edu.kit.pse.fragetool.common.NotFoundException;
import edu.kit.pse.fragetool.question.AnswerContent;
import edu.kit.pse.fragetool.session.Participant;
import edu.kit.pse.fragetool.session.ParticipantRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Annahme von Antworten (/MK240/ bis /MK255/). */
@Service
public class AnswerService {

    private final QuestionRunRepository runs;
    private final AnswerRepository answers;
    private final ParticipantRepository participants;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AnswerService(QuestionRunRepository runs, AnswerRepository answers, ParticipantRepository participants,
            ApplicationEventPublisher events, Clock clock) {
        this.runs = runs;
        this.answers = answers;
        this.participants = participants;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Speichert die Antwort eines Teilnehmers.
     * Kehrt die Methode ohne Ausnahme zurück, ist die Antwort gespeichert (/MK245/).
     *
     * @throws ConflictException wenn die Zeit abgelaufen ist, die Frage beendet wurde
     *                           oder der Teilnehmer schon geantwortet hat
     */
    @Transactional
    public void submit(String token, UUID runId, AnswerRequest request) {
        Instant now = clock.instant();
        Participant participant = participants.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Teilnehmer nicht gefunden."));
        QuestionRun run = runs.findById(runId).orElseThrow(() -> new NotFoundException("Frage nicht gefunden."));
        if (!participant.getSession().getId().equals(run.getSession().getId())) {
            throw new ForbiddenException("Die Frage gehört nicht zu dieser Session.");
        }
        run.ensureAccepting(now);
        if (answers.existsByRunAndParticipant(run, participant)) {
            throw new ConflictException("Du hast diese Frage bereits beantwortet.");
        }
        Set<UUID> optionIds = request.optionIds() == null ? Set.of() : new HashSet<>(request.optionIds());
        AnswerContent content = new AnswerContent(optionIds, request.numericValue(), request.text());
        run.getQuestion().validateAnswer(content);
        answers.save(new Answer(run, participant, content, now));
        events.publishEvent(new AnswerCountChangedEvent(run.getSession().getJoinCode(), answers.countByRun(run)));
    }
}
