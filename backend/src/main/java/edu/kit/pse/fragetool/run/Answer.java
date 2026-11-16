package edu.kit.pse.fragetool.run;

import edu.kit.pse.fragetool.question.AnswerContent;
import edu.kit.pse.fragetool.session.Participant;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Die Antwort eines Teilnehmers auf eine gestartete Frage.
 * Pro Teilnehmer und Frage gibt es höchstens eine Antwort (/MK240/); das sichert auch die Datenbank ab.
 */
@Entity
@Table(name = "answer",
        uniqueConstraints = @UniqueConstraint(name = "uq_answer_once", columnNames = {"run_id", "participant_id"}))
public class Answer {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private QuestionRun run;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Participant participant;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "answer_selection", joinColumns = @JoinColumn(name = "answer_id"))
    @Column(name = "option_id", nullable = false)
    private Set<UUID> selectedOptionIds = new HashSet<>();

    @Column(precision = 30, scale = 10)
    private BigDecimal numericValue;

    @Column(length = 500)
    private String text;

    /** {@code null}, solange die Frage läuft oder wenn sie nicht bewertbar ist. */
    private Boolean correct;

    @Column(nullable = false)
    private Instant submittedAt;

    protected Answer() {
        // für JPA
    }

    public Answer(QuestionRun run, Participant participant, AnswerContent content, Instant now) {
        this.id = UUID.randomUUID();
        this.run = run;
        this.participant = participant;
        this.selectedOptionIds.addAll(content.optionIds());
        this.numericValue = content.numericValue();
        this.text = content.text() == null ? null : content.text().strip();
        this.submittedAt = now;
    }

    public AnswerContent content() {
        return new AnswerContent(selectedOptionIds, numericValue, text);
    }

    void markEvaluated(boolean isCorrect) {
        this.correct = isCorrect;
    }

    public UUID getId() {
        return id;
    }

    public Participant getParticipant() {
        return participant;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public String getText() {
        return text;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
