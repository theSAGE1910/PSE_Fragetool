package edu.kit.pse.fragetool.config;

import edu.kit.pse.fragetool.common.ConflictException;
import edu.kit.pse.fragetool.common.ForbiddenException;
import edu.kit.pse.fragetool.common.NotFoundException;
import edu.kit.pse.fragetool.common.ValidationException;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Übersetzt Ausnahmen in HTTP-Statuscodes mit einer deutschen Meldung: {"message": "..."}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> onValidation(ValidationException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> onUnreadable(HttpMessageNotReadableException e) {
        return body(HttpStatus.BAD_REQUEST, "Die Anfrage ist ungültig.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> onTooLarge(MaxUploadSizeExceededException e) {
        return body(HttpStatus.BAD_REQUEST, "Die Datei ist zu groß (höchstens 5 MB).");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> onAuthentication(AuthenticationException e) {
        return body(HttpStatus.UNAUTHORIZED, "E-Mail oder Passwort falsch.");
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> onForbidden(ForbiddenException e) {
        return body(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> onNotFound(NotFoundException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> onConflict(ConflictException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Greift, wenn eine Datenbankregel verletzt wird, z. B. zwei gleichzeitige Antworten (/MK240/). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> onIntegrity(DataIntegrityViolationException e) {
        return body(HttpStatus.CONFLICT, "Die Aktion wurde bereits ausgeführt.");
    }

    private static ResponseEntity<Map<String, String>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
