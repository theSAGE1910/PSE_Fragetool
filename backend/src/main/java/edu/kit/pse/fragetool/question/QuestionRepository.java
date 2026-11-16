package edu.kit.pse.fragetool.question;

import edu.kit.pse.fragetool.auth.Dozent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    /** Fragenkatalog eines Dozenten ohne Schnellstart-Fragen. */
    List<Question> findByOwnerAndQuickStartFalseOrderByTextAsc(Dozent owner);

    /** Fragenkatalog gefiltert nach einem Tag (/MK120/). */
    @Query("select q from Question q where q.owner = :owner and q.quickStart = false "
            + "and :tag member of q.tags order by q.text asc")
    List<Question> findCatalogByTag(@Param("owner") Dozent owner, @Param("tag") String tag);
}
