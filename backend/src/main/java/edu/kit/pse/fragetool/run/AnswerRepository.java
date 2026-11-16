package edu.kit.pse.fragetool.run;

import edu.kit.pse.fragetool.session.Participant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    List<Answer> findByRunOrderBySubmittedAtAsc(QuestionRun run);

    /** Antworten samt Teilnehmern in einer Abfrage, für die Punktevergabe. */
    @Query("select a from Answer a join fetch a.participant where a.run = :run")
    List<Answer> findWithParticipantByRun(@Param("run") QuestionRun run);

    long countByRun(QuestionRun run);

    boolean existsByRunAndParticipant(QuestionRun run, Participant participant);

    Optional<Answer> findByRunAndParticipant(QuestionRun run, Participant participant);
}
