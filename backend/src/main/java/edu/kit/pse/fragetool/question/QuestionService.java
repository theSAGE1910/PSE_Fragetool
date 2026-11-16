package edu.kit.pse.fragetool.question;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.common.ForbiddenException;
import edu.kit.pse.fragetool.common.NotFoundException;
import edu.kit.pse.fragetool.common.ValidationException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Verwaltung des Fragenkatalogs (/MK50/ bis /MK120/). */
@Service
public class QuestionService {

    private final QuestionRepository questions;

    public QuestionService(QuestionRepository questions) {
        this.questions = questions;
    }

    @Transactional(readOnly = true)
    public List<Question> list(Dozent owner, String tag) {
        if (tag == null || tag.isBlank()) {
            return questions.findByOwnerAndQuickStartFalseOrderByTextAsc(owner);
        }
        return questions.findCatalogByTag(owner, tag.strip().toLowerCase(Locale.ROOT));
    }

    @Transactional
    public Question create(Dozent owner, QuestionRequest request) {
        if (request.type() == null) {
            throw new ValidationException("Der Fragetyp fehlt.");
        }
        Question question = QuestionFactory.create(request.type(), owner, "",
                Question.DEFAULT_TIME_LIMIT_SECONDS, false);
        apply(question, request);
        return questions.save(question);
    }

    @Transactional
    public Question update(Dozent owner, UUID id, QuestionRequest request) {
        Question question = requireOwned(id, owner);
        if (request.type() != null && request.type() != question.getType()) {
            throw new ValidationException("Der Fragetyp kann nachträglich nicht geändert werden.");
        }
        apply(question, request);
        return question;
    }

    @Transactional
    public void delete(Dozent owner, UUID id) {
        questions.delete(requireOwned(id, owner));
    }

    /** Lädt eine Frage und prüft, dass sie dem Dozenten gehört (/MK40/). */
    @Transactional(readOnly = true)
    public Question requireOwned(UUID id, Dozent owner) {
        Question question = questions.findById(id)
                .orElseThrow(() -> new NotFoundException("Frage nicht gefunden."));
        if (!question.isOwnedBy(owner) || question.isQuickStart()) {
            throw new ForbiddenException("Kein Zugriff.");
        }
        return question;
    }

    private static void apply(Question question, QuestionRequest request) {
        int timeLimit = request.timeLimitSeconds() == null
                ? Question.DEFAULT_TIME_LIMIT_SECONDS : request.timeLimitSeconds();
        question.update(request.text(), timeLimit, request.tags());
        question.configure(request.options(), request.correctValue(), request.tolerance());
        question.validate();
    }
}
