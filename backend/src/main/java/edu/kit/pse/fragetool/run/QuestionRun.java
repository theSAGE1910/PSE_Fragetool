package edu.kit.pse.fragetool.run;

import edu.kit.pse.fragetool.common.ConflictException;
import edu.kit.pse.fragetool.question.Question;
import edu.kit.pse.fragetool.session.Session;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Eine in einer Session gestartete Frage mit Start- und Endzeitpunkt.
 *
 * <p>Alle Zeitpunkte kommen vom Server (/MK250/, /MK430/). Die Klasse entscheidet selbst,
 * ob sie noch Antworten annimmt, und bewertet die Antworten beim Schließen.
 */
@Entity
@Table(name = "question_run")
public class QuestionRun {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Session session;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Question question;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant endsAt;

    private Instant closedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RunStatus status;

    /** Ob die Freitextantworten in der Präsentationsansicht sichtbar sind (/MK295/). */
    @Column(nullable = false)
    private boolean freeTextReleased;

    protected QuestionRun() {
        // für JPA
    }

    public QuestionRun(Session session, Question question, Instant now) {
        this.id = UUID.randomUUID();
        this.session = session;
        this.question = question;
        this.startedAt = now;
        this.endsAt = now.plusSeconds(question.getTimeLimitSeconds());
        this.status = RunStatus.RUNNING;
    }

    public boolean isRunning() {
        return status == RunStatus.RUNNING;
    }

    /**
     * Prüft, ob zum Zeitpunkt {@code now} noch eine Antwort angenommen wird.
     *
     * @throws ConflictException wenn die Frage beendet (/MK255/) oder die Zeit abgelaufen ist (/MK250/)
     */
    public void ensureAccepting(Instant now) {
        if (!isRunning()) {
            throw new ConflictException("Frage beendet.");
        }
        if (!now.isBefore(endsAt)) {
            throw new ConflictException("Zeit abgelaufen.");
        }
    }

    /**
     * Beendet die Frage, bewertet alle Antworten und vergibt Punkte (/MK310/).
     * Ein zweiter Aufruf hat keine Wirkung.
     */
    public void closeAndEvaluate(Instant now, Collection<Answer> answers, ScoringStrategy scoring) {
        if (!isRunning()) {
            return;
        }
        this.status = RunStatus.CLOSED;
        this.closedAt = now;
        if (!question.isGradable()) {
            return;
        }
        for (Answer answer : answers) {
            boolean isCorrect = question.evaluate(answer.content());
            answer.markEvaluated(isCorrect);
            if (isCorrect) {
                answer.getParticipant().addPoints(scoring.pointsFor(this, answer));
            }
        }
    }

    public void setFreeTextReleased(boolean released) {
        this.freeTextReleased = released;
    }

    public UUID getId() {
        return id;
    }

    public Session getSession() {
        return session;
    }

    public Question getQuestion() {
        return question;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public RunStatus getStatus() {
        return status;
    }

    public boolean isFreeTextReleased() {
        return freeTextReleased;
    }
}
