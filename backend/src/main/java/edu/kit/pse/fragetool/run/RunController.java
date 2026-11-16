package edu.kit.pse.fragetool.run;

import edu.kit.pse.fragetool.auth.DozentService;
import edu.kit.pse.fragetool.question.QuestionType;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST-Schnittstelle zum Steuern von Fragen. Nur für angemeldete Dozenten. */
@RestController
@RequestMapping("/api")
public class RunController {

    public record StartRequest(UUID questionId) {
    }

    public record QuickStartRequest(QuestionType type, Integer optionCount, Integer timeLimitSeconds) {
    }

    public record ReleaseRequest(boolean released) {
    }

    private final RunService runs;
    private final DozentService dozents;

    public RunController(RunService runs, DozentService dozents) {
        this.runs = runs;
        this.dozents = dozents;
    }

    @PostMapping("/sessions/{sessionId}/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> start(Principal principal, @PathVariable UUID sessionId,
            @RequestBody StartRequest request) {
        QuestionRun run = runs.start(dozents.require(principal), sessionId, request.questionId());
        return Map.of("runId", run.getId());
    }

    @PostMapping("/sessions/{sessionId}/quickstart")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> quickStart(Principal principal, @PathVariable UUID sessionId,
            @RequestBody QuickStartRequest request) {
        QuestionRun run = runs.quickStart(dozents.require(principal), sessionId, request.type(),
                request.optionCount(), request.timeLimitSeconds());
        return Map.of("runId", run.getId());
    }

    @PostMapping("/runs/{runId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(Principal principal, @PathVariable UUID runId) {
        runs.close(dozents.require(principal), runId);
    }

    @PutMapping("/runs/{runId}/freetext-released")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setFreeTextReleased(Principal principal, @PathVariable UUID runId,
            @RequestBody ReleaseRequest request) {
        runs.setFreeTextReleased(dozents.require(principal), runId, request.released());
    }

    @GetMapping("/runs/{runId}/freetext")
    public List<String> freeTextAnswers(Principal principal, @PathVariable UUID runId) {
        return runs.freeTextAnswers(dozents.require(principal), runId);
    }
}
