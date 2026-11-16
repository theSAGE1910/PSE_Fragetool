package edu.kit.pse.fragetool.session;

import edu.kit.pse.fragetool.auth.Dozent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findByOwnerOrderByCreatedAtDesc(Dozent owner);

    Optional<Session> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);
}
