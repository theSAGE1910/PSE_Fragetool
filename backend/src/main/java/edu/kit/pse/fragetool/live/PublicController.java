package edu.kit.pse.fragetool.live;

import edu.kit.pse.fragetool.run.AnswerRequest;
import edu.kit.pse.fragetool.run.AnswerService;
import edu.kit.pse.fragetool.session.Participant;
import edu.kit.pse.fragetool.session.ParticipantService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Schnittstelle ohne Anmeldung: für Teilnehmer und für die Präsentationsansicht.
 * Teilnehmer weisen sich mit ihrem Token im Header X-Participant-Token aus (/MK180/).
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    public static final String TOKEN_HEADER = "X-Participant-Token";

    public record JoinResponse(String displayName, String token) {
    }

    private final SessionStateService states;
    private final ParticipantService participants;
    private final AnswerService answers;

    public PublicController(SessionStateService states, ParticipantService participants, AnswerService answers) {
        this.states = states;
        this.participants = participants;
        this.answers = answers;
    }

    @GetMapping("/sessions/{joinCode}/state")
    public SessionStateDto state(@PathVariable String joinCode) {
        return states.state(joinCode);
    }

    @PostMapping("/sessions/{joinCode}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    public JoinResponse join(@PathVariable String joinCode) {
        Participant participant = participants.join(joinCode);
        return new JoinResponse(participant.getDisplayName(), participant.getToken());
    }

    @GetMapping("/participants/me")
    public MeDto me(@RequestHeader(TOKEN_HEADER) String token) {
        return states.me(token);
    }

    /** Antwortet mit 201, sobald die Antwort gespeichert ist (/MK245/); sonst mit 409 (/MK250/, /MK255/). */
    @PostMapping("/runs/{runId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public void submit(@RequestHeader(TOKEN_HEADER) String token, @PathVariable UUID runId,
            @RequestBody AnswerRequest request) {
        answers.submit(token, runId, request);
    }
}
