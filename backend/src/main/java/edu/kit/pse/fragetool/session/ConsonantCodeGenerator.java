package edu.kit.pse.fragetool.session;

import java.util.Random;

/**
 * Anzeigenamen aus Konsonanten und Ziffern (/MK171/).
 *
 * <p>Ohne Vokale können keine Wörter entstehen. Deshalb ist kein Filter für
 * unangemessene Namen nötig. Die Zeichen 0, O, 1 und I fehlen, weil sie leicht
 * verwechselt werden.
 */
public class ConsonantCodeGenerator implements DisplayNameGenerator {

    public static final String ALPHABET = "BCDFGHJKLMNPQRSTVWXZ23456789";

    private final Random random;
    private final DisplayNameLengthPolicy lengthPolicy;

    public ConsonantCodeGenerator(Random random, DisplayNameLengthPolicy lengthPolicy) {
        this.random = random;
        this.lengthPolicy = lengthPolicy;
    }

    @Override
    public String generate(long participantCount) {
        return randomCode(lengthPolicy.lengthFor(participantCount));
    }

    /** Zufälliger Code fester Länge, auch für Beitrittscodes von Sessions verwendbar. */
    public String randomCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
