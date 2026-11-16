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

/**
 * Das Bild einer Frage (/MK155/). Die Bilddaten liegen in der Datenbank; so genügt ein
 * einziges Backup und beim Löschen der Frage verschwindet das Bild mit.
 */
@Entity
@Table(name = "question_image")
public class QuestionImage {

    private static final int MAX_STORED_BYTES = 6 * 1024 * 1024;

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Question question;

    @Column(nullable = false, length = 32)
    private String contentType;

    @Column(nullable = false, length = MAX_STORED_BYTES)
    private byte[] data;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    protected QuestionImage() {
        // für JPA
    }

    public QuestionImage(Question question, ImageProcessor.ProcessedImage image) {
        this.id = UUID.randomUUID();
        this.question = question;
        this.contentType = image.contentType();
        this.data = image.data();
        this.width = image.width();
        this.height = image.height();
    }

    public UUID getId() {
        return id;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }
}
