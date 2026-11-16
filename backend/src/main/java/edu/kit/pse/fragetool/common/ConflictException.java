package edu.kit.pse.fragetool.common;

/** Die Aktion passt nicht zum aktuellen Zustand. Wird als HTTP 409 gemeldet. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
