package edu.kit.pse.fragetool.session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    Optional<Participant> findByToken(String token);

    List<Participant> findBySession(Session session);

    long countBySession(Session session);

    boolean existsBySessionAndDisplayName(Session session, String displayName);
}
