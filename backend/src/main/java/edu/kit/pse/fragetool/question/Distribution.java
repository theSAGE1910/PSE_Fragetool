package edu.kit.pse.fragetool.question;

import java.util.List;

/** Antwortverteilung einer Frage (/MK290/). */
public record Distribution(List<Entry> entries) {

    /**
     * Eine Zeile der Verteilung.
     *
     * @param label   Antworttext, Zahlenwert oder Freitext
     * @param count   Anzahl der Teilnehmer mit dieser Antwort
     * @param correct ob die Antwort korrekt ist; {@code null} bei nicht bewertbaren Fragen
     */
    public record Entry(String label, long count, Boolean correct) {
    }

    public Distribution {
        entries = List.copyOf(entries);
    }
}
