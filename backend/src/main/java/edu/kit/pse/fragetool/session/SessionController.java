package edu.kit.pse.fragetool.session;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST-Schnittstelle für Sessions. Nur für angemeldete Dozenten. */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    public record CreateSessionRequest(String name) {
    }

    public record VisibilityRequest(boolean visible) {
    }

    private final SessionService sessions;
    private final DozentService dozents;

    public SessionController(SessionService sessions, DozentService dozents) {
        this.sessions = sessions;
        this.dozents = dozents;
    }

    @GetMapping
    public List<SessionDto> list(Principal principal) {
        return sessions.list(dozents.require(principal)).stream().map(SessionDto::from).toList();
    }

    @GetMapping("/{id}")
    public SessionDto get(Principal principal, @PathVariable UUID id) {
        return SessionDto.from(sessions.requireOwned(id, dozents.require(principal)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionDto create(Principal principal, @RequestBody CreateSessionRequest request) {
        return SessionDto.from(sessions.create(dozents.require(principal), request.name()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable UUID id) {
        sessions.delete(dozents.require(principal), id);
    }

    /** Blendet das Scoreboard in der Präsentationsansicht ein oder aus (/MK320/). */
    @PutMapping("/{id}/scoreboard-visible")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setScoreboardVisible(Principal principal, @PathVariable UUID id,
            @RequestBody VisibilityRequest request) {
        sessions.setScoreboardVisible(dozents.require(principal), id, request.visible());
    }
}
