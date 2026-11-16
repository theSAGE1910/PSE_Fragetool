package edu.kit.pse.fragetool.common;

/** Das angefragte Objekt existiert nicht. Wird als HTTP 404 gemeldet. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
