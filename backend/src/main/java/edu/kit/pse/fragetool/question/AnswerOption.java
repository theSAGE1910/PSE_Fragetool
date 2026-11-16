package edu.kit.pse.fragetool.question;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** Eine Antwortmöglichkeit einer Single- oder Multiple-Choice-Frage. */
@Entity
@Table(name = "answer_option")
public class AnswerOption {

    public static final int MAX_TEXT_LENGTH = 500;

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ChoiceQuestion question;

    @Column(nullable = false, length = MAX_TEXT_LENGTH)
    private String text;

    @Column(nullable = false)
    private boolean correct;

    @Column(nullable = false)
    private int sortIndex;

    protected AnswerOption() {
        // für JPA
    }

    AnswerOption(ChoiceQuestion question, String text, boolean correct, int sortIndex) {
        this.id = UUID.randomUUID();
        this.question = question;
        this.text = text;
        this.correct = correct;
        this.sortIndex = sortIndex;
    }

    public UUID getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }

    public int getSortIndex() {
        return sortIndex;
    }
}
