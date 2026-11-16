package edu.kit.pse.fragetool.session;

import java.time.Instant;
import java.util.UUID;

/** Eine Session in der Übersicht des Dozenten. */
public record SessionDto(UUID id, String name, String joinCode, Instant createdAt) {

    public static SessionDto from(Session session) {
        return new SessionDto(session.getId(), session.getName(), session.getJoinCode(), session.getCreatedAt());
    }
}
