package edu.kit.pse.fragetool.run;

import edu.kit.pse.fragetool.session.Session;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRunRepository extends JpaRepository<QuestionRun, UUID> {

    /** Die zuletzt gestartete Frage einer Session (laufend oder beendet). */
    Optional<QuestionRun> findFirstBySessionOrderByStartedAtDesc(Session session);

    boolean existsBySessionAndStatus(Session session, RunStatus status);

    List<QuestionRun> findByStatus(RunStatus status);
}
