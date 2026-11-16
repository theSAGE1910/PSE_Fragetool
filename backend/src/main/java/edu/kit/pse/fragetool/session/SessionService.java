package edu.kit.pse.fragetool.session;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.common.ForbiddenException;
import edu.kit.pse.fragetool.common.NotFoundException;
import edu.kit.pse.fragetool.common.SessionChangedEvent;
import edu.kit.pse.fragetool.common.ValidationException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Verwaltung der Sessions eines Dozenten (/MK130/, /MK140/). */
@Service
public class SessionService {

    private static final int JOIN_CODE_LENGTH = 6;

    private final SessionRepository sessions;
    private final ConsonantCodeGenerator codeGenerator;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public SessionService(SessionRepository sessions, ConsonantCodeGenerator codeGenerator,
            ApplicationEventPublisher events, Clock clock) {
        this.sessions = sessions;
        this.codeGenerator = codeGenerator;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Session> list(Dozent owner) {
        return sessions.findByOwnerOrderByCreatedAtDesc(owner);
    }

    @Transactional
    public Session create(Dozent owner, String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Der Name der Session darf nicht leer sein.");
        }
        if (name.length() > Session.MAX_NAME_LENGTH) {
            throw new ValidationException("Der Name der Session ist zu lang.");
        }
        String joinCode = codeGenerator.randomCode(JOIN_CODE_LENGTH);
        while (sessions.existsByJoinCode(joinCode)) {
            joinCode = codeGenerator.randomCode(JOIN_CODE_LENGTH);
        }
        return sessions.save(new Session(owner, name.strip(), joinCode, clock.instant()));
    }

    /** Löscht die Session samt Teilnehmern, Fragenläufen und Antworten (ON DELETE CASCADE). */
    @Transactional
    public void delete(Dozent owner, UUID id) {
        Session session = requireOwned(id, owner);
        sessions.delete(session);
        events.publishEvent(new SessionChangedEvent(session.getJoinCode()));
    }

    @Transactional
    public void setScoreboardVisible(Dozent owner, UUID id, boolean visible) {
        Session session = requireOwned(id, owner);
        session.setScoreboardVisible(visible);
        events.publishEvent(new SessionChangedEvent(session.getJoinCode()));
    }

    /** Lädt eine Session und prüft, dass sie dem Dozenten gehört (/MK40/). */
    @Transactional(readOnly = true)
    public Session requireOwned(UUID id, Dozent owner) {
        Session session = sessions.findById(id)
                .orElseThrow(() -> new NotFoundException("Session nicht gefunden."));
        if (!session.isOwnedBy(owner)) {
            throw new ForbiddenException("Kein Zugriff.");
        }
        return session;
    }

    @Transactional(readOnly = true)
    public Session requireByJoinCode(String joinCode) {
        return sessions.findByJoinCode(joinCode)
                .orElseThrow(() -> new NotFoundException("Session nicht gefunden."));
    }
}
