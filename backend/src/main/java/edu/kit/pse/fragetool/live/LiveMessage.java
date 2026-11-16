package edu.kit.pse.fragetool.live;

import java.time.Instant;

/**
 * Benachrichtigung an alle Ansichten einer Session.
 *
 * @param type      STATE_CHANGED: Zustand neu laden; ANSWER_COUNT / PARTICIPANT_COUNT: neuer Zählerstand in value
 * @param value     Zählerstand, sonst {@code null}
 * @param serverNow Serverzeit beim Senden
 */
public record LiveMessage(String type, Long value, Instant serverNow) {

    public static final String STATE_CHANGED = "STATE_CHANGED";
    public static final String ANSWER_COUNT = "ANSWER_COUNT";
    public static final String PARTICIPANT_COUNT = "PARTICIPANT_COUNT";
}
