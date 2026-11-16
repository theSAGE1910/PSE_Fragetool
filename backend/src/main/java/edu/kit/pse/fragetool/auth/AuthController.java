package edu.kit.pse.fragetool.auth;

import edu.kit.pse.fragetool.common.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registrierung, Anmeldung und Abfrage des angemeldeten Dozenten. Abmelden: POST /api/auth/logout. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record Credentials(String email, String password) {
    }

    private final DozentService dozents;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(DozentService dozents, AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.dozents = dozents;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@RequestBody Credentials credentials) {
        Dozent dozent = dozents.register(credentials.email(), credentials.password());
        return Map.of("email", dozent.getEmail());
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Credentials credentials, HttpServletRequest request,
            HttpServletResponse response) {
        if (credentials.email() == null || credentials.password() == null) {
            throw new ValidationException("E-Mail-Adresse und Passwort sind erforderlich.");
        }
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        DozentService.normalize(credentials.email()), credentials.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return Map.of("email", authentication.getName());
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("email", principal.getName()));
    }
}
