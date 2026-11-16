package edu.kit.pse.fragetool.session;

import edu.kit.pse.fragetool.common.NotFoundException;
import edu.kit.pse.fragetool.common.ParticipantCountChangedEvent;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Beitritt von Teilnehmern ohne Registrierung (/MK160/ bis /MK180/). */
@Service
public class ParticipantService {

    private static final int MAX_NAME_ATTEMPTS = 20;
    private static final int TOKEN_BYTES = 32;

    private final SessionRepository sessions;
    private final ParticipantRepository participants;
    private final DisplayNameGenerator nameGenerator;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public ParticipantService(SessionRepository sessions, ParticipantRepository participants,
            DisplayNameGenerator nameGenerator, ApplicationEventPublisher events, Clock clock) {
        this.sessions = sessions;
        this.participants = participants;
        this.nameGenerator = nameGenerator;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Legt einen Teilnehmer mit automatisch vergebenem Anzeigenamen an.
     *
     * <p>Die Methode ist bewusst nicht transaktional: Jeder Versuch ist eine eigene Transaktion.
     * Vergibt die Datenbank denselben Namen gleichzeitig an einen anderen Teilnehmer
     * (UNIQUE-Regel), wird mit einem neuen Namen wiederholt (/MK173/).
     */
    public Participant join(String joinCode) {
        Session session = sessions.findByJoinCode(joinCode)
                .orElseThrow(() -> new NotFoundException("Session nicht gefunden."));
        for (int attempt = 0; attempt < MAX_NAME_ATTEMPTS; attempt++) {
            long count = participants.countBySession(session) + 1;
            String name = nameGenerator.generate(count);
            if (participants.existsBySessionAndDisplayName(session, name)) {
                continue;
            }
            try {
                Participant participant = participants.saveAndFlush(
                        new Participant(session, name, newToken(), clock.instant()));
                events.publishEvent(new ParticipantCountChangedEvent(joinCode, count));
                return participant;
            } catch (DataIntegrityViolationException nameTakenMeanwhile) {
                // nächster Versuch mit neuem Namen
            }
        }
        throw new IllegalStateException("Kein freier Anzeigename gefunden.");
    }

    public Participant requireByToken(String token) {
        return participants.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Teilnehmer nicht gefunden."));
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
