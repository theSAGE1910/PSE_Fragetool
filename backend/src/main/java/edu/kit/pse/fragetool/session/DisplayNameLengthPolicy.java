package edu.kit.pse.fragetool.session;

/**
 * Legt die Länge der Anzeigenamen fest (/MK172/).
 *
 * <p>Die Länge ist die kleinste Zahl L >= 3, für die ALPHABET_SIZE^L >= 10 * n gilt.
 * So sind höchstens 10 % der möglichen Namen vergeben und Kollisionen bleiben selten.
 * Ergebnis: 3 Zeichen bis 2.195 Teilnehmer, 4 Zeichen bis 61.465 Teilnehmer.
 */
public class DisplayNameLengthPolicy {

    public static final int MIN_LENGTH = 3;
    private static final int FREE_NAMES_FACTOR = 10;

    public int lengthFor(long participantCount) {
        long needed = Math.max(1, participantCount) * FREE_NAMES_FACTOR;
        int length = MIN_LENGTH;
        long capacity = pow(ConsonantCodeGenerator.ALPHABET.length(), length);
        while (capacity < needed) {
            length++;
            capacity *= ConsonantCodeGenerator.ALPHABET.length();
        }
        return length;
    }

    private static long pow(int base, int exponent) {
        long result = 1;
        for (int i = 0; i < exponent; i++) {
            result *= base;
        }
        return result;
    }
}
