package edu.kit.pse.fragetool.question;

/** Eingabe für eine Antwortmöglichkeit: Text und Kennzeichnung als korrekt. */
public record OptionSpec(String text, boolean correct) {
}
