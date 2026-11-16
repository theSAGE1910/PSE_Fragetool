package edu.kit.pse.fragetool.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DozentRepository extends JpaRepository<Dozent, UUID> {

    Optional<Dozent> findByEmail(String email);

    boolean existsByEmail(String email);
}
