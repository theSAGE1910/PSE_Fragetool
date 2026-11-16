package edu.kit.pse.fragetool.auth;

import edu.kit.pse.fragetool.common.ConflictException;
import edu.kit.pse.fragetool.common.ForbiddenException;
import edu.kit.pse.fragetool.common.ValidationException;
import java.security.Principal;
import java.time.Clock;
import java.util.Locale;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registrierung und Anmeldung von Dozenten (/MK10/ bis /MK30/). */
@Service
public class DozentService implements UserDetailsService {

    public static final int MIN_PASSWORD_LENGTH = 8;

    private final DozentRepository dozents;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DozentService(DozentRepository dozents, PasswordEncoder passwordEncoder, Clock clock) {
        this.dozents = dozents;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public Dozent register(String email, String password) {
        String normalized = normalize(email);
        if (normalized.isEmpty() || !normalized.contains("@")) {
            throw new ValidationException("Bitte eine gültige E-Mail-Adresse eingeben.");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Das Passwort muss mindestens 8 Zeichen haben.");
        }
        if (dozents.existsByEmail(normalized)) {
            throw new ConflictException("E-Mail-Adresse bereits vergeben.");
        }
        return dozents.save(new Dozent(normalized, passwordEncoder.encode(password), clock.instant()));
    }

    /** Der angemeldete Dozent zu einer Anfrage. */
    @Transactional(readOnly = true)
    public Dozent require(Principal principal) {
        if (principal == null) {
            throw new ForbiddenException("Nicht angemeldet.");
        }
        return dozents.findByEmail(principal.getName())
                .orElseThrow(() -> new ForbiddenException("Konto nicht gefunden."));
    }

    /** Wird von Spring Security beim Login aufgerufen. */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        Dozent dozent = dozents.findByEmail(normalize(username))
                .orElseThrow(() -> new UsernameNotFoundException("Unbekannte E-Mail-Adresse."));
        return User.withUsername(dozent.getEmail()).password(dozent.getPasswordHash()).roles("DOZENT").build();
    }

    public static String normalize(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
