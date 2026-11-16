package edu.kit.pse.fragetool.question;

import edu.kit.pse.fragetool.auth.DozentService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST-Schnittstelle des Fragenkatalogs. Nur für angemeldete Dozenten. */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questions;
    private final DozentService dozents;

    public QuestionController(QuestionService questions, DozentService dozents) {
        this.questions = questions;
        this.dozents = dozents;
    }

    @GetMapping
    public List<QuestionDto> list(Principal principal, @RequestParam(required = false) String tag) {
        return questions.list(dozents.require(principal), tag).stream().map(QuestionDto::from).toList();
    }

    @GetMapping("/{id}")
    public QuestionDto get(Principal principal, @PathVariable UUID id) {
        return QuestionDto.from(questions.requireOwned(id, dozents.require(principal)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionDto create(Principal principal, @RequestBody QuestionRequest request) {
        return QuestionDto.from(questions.create(dozents.require(principal), request));
    }

    @PutMapping("/{id}")
    public QuestionDto update(Principal principal, @PathVariable UUID id, @RequestBody QuestionRequest request) {
        return QuestionDto.from(questions.update(dozents.require(principal), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable UUID id) {
        questions.delete(dozents.require(principal), id);
    }
}
