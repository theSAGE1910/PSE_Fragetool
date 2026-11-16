package edu.kit.pse.fragetool.common;

/** Fachlich ungültige Eingabe. Wird als HTTP 400 gemeldet. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
