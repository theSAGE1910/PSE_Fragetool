package edu.kit.pse.fragetool.session;

/** Erzeugt Anzeigenamen für Teilnehmer (/MK170/). Austauschbar, z. B. durch eine Wortliste. */
public interface DisplayNameGenerator {

    /**
     * @param participantCount Anzahl der Teilnehmer der Session einschließlich des neuen Teilnehmers
     * @return ein neuer Anzeigename; die Eindeutigkeit prüft der Aufrufer
     */
    String generate(long participantCount);
}
