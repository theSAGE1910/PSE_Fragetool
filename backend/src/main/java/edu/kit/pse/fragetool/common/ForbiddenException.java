package edu.kit.pse.fragetool.common;

/** Zugriff auf fremde Daten (/MK40/). Wird als HTTP 403 gemeldet. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
