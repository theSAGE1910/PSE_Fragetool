package edu.kit.pse.fragetool.run;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.common.ConflictException;
import edu.kit.pse.fragetool.common.ForbiddenException;
import edu.kit.pse.fragetool.common.NotFoundException;
import edu.kit.pse.fragetool.common.SessionChangedEvent;
import edu.kit.pse.fragetool.common.ValidationException;
import edu.kit.pse.fragetool.question.ChoiceQuestion;
import edu.kit.pse.fragetool.question.OptionSpec;
import edu.kit.pse.fragetool.question.Question;
import edu.kit.pse.fragetool.question.QuestionFactory;
import edu.kit.pse.fragetool.question.QuestionRepository;
import edu.kit.pse.fragetool.question.QuestionService;
import edu.kit.pse.fragetool.question.QuestionType;
import edu.kit.pse.fragetool.session.Session;
import edu.kit.pse.fragetool.session.SessionService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Starten und Beenden von Fragen in einer Session (/MK190/ bis /MK260/, /MK295/). */
@Service
public class RunService {

    private static final String QUICK_START_TEXT = "Schnellstart-Frage";
    private static final int DEFAULT_QUICK_START_OPTIONS = 4;

    private final SessionService sessions;
    private final QuestionService questions;
    private final QuestionRepository questionRepository;
    private final QuestionRunRepository runs;
    private final AnswerRepository answers;
    private final RunCloser closer;
    private final RunScheduler scheduler;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RunService(SessionService sessions, QuestionService questions, QuestionRepository questionRepository,
            QuestionRunRepository runs, AnswerRepository answers, RunCloser closer, RunScheduler scheduler,
            ApplicationEventPublisher events, Clock clock) {
        this.sessions = sessions;
        this.questions = questions;
        this.questionRepository = questionRepository;
        this.runs = runs;
        this.answers = answers;
        this.closer = closer;
        this.scheduler = scheduler;
        this.events = events;
        this.clock = clock;
    }

    /** Startet eine Frage aus dem Fragenkatalog (/MK190/). */
    @Transactional
    public QuestionRun start(Dozent dozent, UUID sessionId, UUID questionId) {
        if (questionId == null) {
            throw new ValidationException("Die Frage fehlt.");
        }
        Session session = sessions.requireOwned(sessionId, dozent);
        Question question = questions.requireOwned(questionId, dozent);
        return startRun(session, question);
    }

    /** Startet eine nicht bewertete Schnellstart-Frage ohne Katalogeintrag (/MK200/). */
    @Transactional
    public QuestionRun quickStart(Dozent dozent, UUID sessionId, QuestionType type, Integer optionCount,
            Integer timeLimitSeconds) {
        Session session = sessions.requireOwned(sessionId, dozent);
        if (type == null) {
            throw new ValidationException("Der Fragetyp fehlt.");
        }
        int seconds = timeLimitSeconds == null ? Question.DEFAULT_TIME_LIMIT_SECONDS : timeLimitSeconds;
        Question question = QuestionFactory.create(type, dozent, QUICK_START_TEXT, seconds, true);
        boolean hasOptions = type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE;
        question.configure(hasOptions ? letterOptions(optionCount) : List.of(), null, null);
        question.validate();
        return startRun(session, questionRepository.save(question));
    }

    /** Beendet eine laufende Frage vorzeitig (/MK260/). */
    @Transactional
    public void close(Dozent dozent, UUID runId) {
        QuestionRun run = requireOwned(runId, dozent);
        scheduler.cancel(run.getId());
        closer.close(run.getId());
    }

    /** Gibt die Liste der Freitextantworten für die Präsentationsansicht frei oder blendet sie aus (/MK295/). */
    @Transactional
    public void setFreeTextReleased(Dozent dozent, UUID runId, boolean released) {
        QuestionRun run = requireOwned(runId, dozent);
        run.setFreeTextReleased(released);
        events.publishEvent(new SessionChangedEvent(run.getSession().getJoinCode()));
    }

    /** Freitextantworten für die Dozentenansicht, unabhängig von der Freigabe (/MK290/). */
    @Transactional(readOnly = true)
    public List<String> freeTextAnswers(Dozent dozent, UUID runId) {
        QuestionRun run = requireOwned(runId, dozent);
        return answers.findByRunOrderBySubmittedAtAsc(run).stream()
                .map(Answer::getText).filter(text -> text != null).toList();
    }

    private QuestionRun startRun(Session session, Question question) {
        if (runs.existsBySessionAndStatus(session, RunStatus.RUNNING)) {
            throw new ConflictException("Es läuft bereits eine Frage.");
        }
        session.setScoreboardVisible(false);
        QuestionRun run = runs.save(new QuestionRun(session, question, clock.instant()));
        scheduler.schedule(run.getId(), run.getEndsAt());
        events.publishEvent(new SessionChangedEvent(session.getJoinCode()));
        return run;
    }

    private QuestionRun requireOwned(UUID runId, Dozent dozent) {
        QuestionRun run = runs.findById(runId).orElseThrow(() -> new NotFoundException("Frage nicht gefunden."));
        if (!run.getSession().isOwnedBy(dozent)) {
            throw new ForbiddenException("Kein Zugriff.");
        }
        return run;
    }

    /** Antwortmöglichkeiten "A", "B", ... für Schnellstart-Fragen. */
    private static List<OptionSpec> letterOptions(Integer optionCount) {
        int count = optionCount == null ? DEFAULT_QUICK_START_OPTIONS : optionCount;
        if (count < ChoiceQuestion.MIN_OPTIONS || count > ChoiceQuestion.MAX_OPTIONS) {
            throw new ValidationException("Die Anzahl der Antwortmöglichkeiten muss zwischen 2 und 6 liegen.");
        }
        List<OptionSpec> options = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            options.add(new OptionSpec(String.valueOf((char) ('A' + i)), false));
        }
        return options;
    }
}
