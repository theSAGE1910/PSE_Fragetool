package edu.kit.pse.fragetool.question;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.common.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bilder an Fragen anhängen, ersetzen und entfernen (/MK155/, /MK156/). */
@Service
public class ImageService {

    private final QuestionService questions;
    private final QuestionImageRepository images;
    private final ImageProcessor processor;

    public ImageService(QuestionService questions, QuestionImageRepository images, ImageProcessor processor) {
        this.questions = questions;
        this.images = images;
        this.processor = processor;
    }

    /** Hängt ein Bild an; ein vorhandenes Bild wird ersetzt. Ungültige Dateien ändern nichts. */
    @Transactional
    public UUID attach(Dozent owner, UUID questionId, byte[] bytes) {
        Question question = questions.requireOwned(questionId, owner);
        ImageProcessor.ProcessedImage processed = processor.process(bytes);
        UUID previous = question.getImageId();
        QuestionImage image = images.save(new QuestionImage(question, processed));
        question.attachImage(image.getId());
        if (previous != null) {
            images.deleteById(previous);
        }
        return image.getId();
    }

    @Transactional
    public void remove(Dozent owner, UUID questionId) {
        Question question = questions.requireOwned(questionId, owner);
        UUID previous = question.getImageId();
        question.removeImage();
        if (previous != null) {
            images.deleteById(previous);
        }
    }

    @Transactional(readOnly = true)
    public QuestionImage load(UUID imageId) {
        return images.findById(imageId).orElseThrow(() -> new NotFoundException("Bild nicht gefunden."));
    }
}
