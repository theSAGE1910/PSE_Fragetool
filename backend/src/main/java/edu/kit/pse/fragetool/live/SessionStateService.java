package edu.kit.pse.fragetool.live;

import edu.kit.pse.fragetool.common.NotFoundException;
import edu.kit.pse.fragetool.live.SessionStateDto.OptionView;
import edu.kit.pse.fragetool.live.SessionStateDto.QuestionView;
import edu.kit.pse.fragetool.live.SessionStateDto.ResultView;
import edu.kit.pse.fragetool.live.SessionStateDto.RunDto;
import edu.kit.pse.fragetool.question.Distribution;
import edu.kit.pse.fragetool.question.Question;
import edu.kit.pse.fragetool.question.QuestionType;
import edu.kit.pse.fragetool.run.Answer;
import edu.kit.pse.fragetool.run.AnswerRepository;
import edu.kit.pse.fragetool.run.QuestionRun;
import edu.kit.pse.fragetool.run.QuestionRunRepository;
import edu.kit.pse.fragetool.scoreboard.Scoreboard;
import edu.kit.pse.fragetool.scoreboard.ScoreboardEntry;
import edu.kit.pse.fragetool.session.Participant;
import edu.kit.pse.fragetool.session.ParticipantRepository;
import edu.kit.pse.fragetool.session.Session;
import edu.kit.pse.fragetool.session.SessionRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baut den aktuellen Zustand einer Session zusammen.
 * Die Ansichten rufen ihn beim Öffnen, nach jeder STATE_CHANGED-Nachricht und nach
 * einer Wiederverbindung ab. Dadurch geht auch nach einem Verbindungsabbruch nichts verloren.
 */
@Service
public class SessionStateService {

    private final SessionRepository sessions;
    private final ParticipantRepository participants;
    private final QuestionRunRepository runs;
    private final AnswerRepository answers;
    private final Clock clock;

    public SessionStateService(SessionRepository sessions, ParticipantRepository participants,
            QuestionRunRepository runs, AnswerRepository answers, Clock clock) {
        this.sessions = sessions;
        this.participants = participants;
        this.runs = runs;
        this.answers = answers;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SessionStateDto state(String joinCode) {
        Session session = sessions.findByJoinCode(joinCode)
                .orElseThrow(() -> new NotFoundException("Session nicht gefunden."));
        List<ScoreboardEntry> scoreboard = session.isScoreboardVisible()
                ? Scoreboard.of(participants.findBySession(session)).top(Scoreboard.PRESENTATION_SIZE)
                : List.of();
        RunDto run = runs.findFirstBySessionOrderByStartedAtDesc(session).map(this::toDto).orElse(null);
        return new SessionStateDto(session.getName(), session.getJoinCode(), clock.instant(),
                participants.countBySession(session), session.isScoreboardVisible(), scoreboard, run);
    }

    @Transactional(readOnly = true)
    public MeDto me(String token) {
        Participant participant = participants.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Teilnehmer nicht gefunden."));
        Session session = participant.getSession();
        Scoreboard scoreboard = Scoreboard.of(participants.findBySession(session));
        int rank = scoreboard.entryOf(participant.getDisplayName()).map(ScoreboardEntry::rank).orElse(0);
        Optional<QuestionRun> lastRun = runs.findFirstBySessionOrderByStartedAtDesc(session);
        Optional<Answer> answer = lastRun.flatMap(run -> answers.findByRunAndParticipant(run, participant));
        return new MeDto(participant.getDisplayName(), participant.getPoints(), rank, scoreboard.size(),
                session.getName(), session.getJoinCode(),
                lastRun.map(QuestionRun::getId).orElse(null),
                answer.isPresent(),
                answer.map(Answer::getCorrect).orElse(null));
    }

    private RunDto toDto(QuestionRun run) {
        Question question = run.getQuestion();
        QuestionView view = new QuestionView(question.getType(), question.getText(), question.isQuickStart(),
                question.isGradable(), question.getImageUrl(),
                question.getOptions().stream().map(o -> new OptionView(o.getId(), o.getText())).toList());
        return new RunDto(run.getId(), run.getStatus(), run.getStartedAt(), run.getEndsAt(),
                answers.countByRun(run), run.isFreeTextReleased(), view,
                run.isRunning() ? null : result(run, question));
    }

    private ResultView result(QuestionRun run, Question question) {
        boolean hidden = question.getType() == QuestionType.FREE_TEXT && !run.isFreeTextReleased();
        Distribution distribution = hidden
                ? new Distribution(List.of())
                : question.distribution(answers.findByRunOrderBySubmittedAtAsc(run).stream()
                        .map(Answer::content).toList());
        return new ResultView(distribution.entries(), question.solutionText());
    }
}
