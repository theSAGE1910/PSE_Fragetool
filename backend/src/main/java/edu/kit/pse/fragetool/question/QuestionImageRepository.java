package edu.kit.pse.fragetool.question;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionImageRepository extends JpaRepository<QuestionImage, UUID> {
}
