package edu.kit.pse.fragetool.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Prüft /MK171/ und /MK172/. */
class DisplayNameTest {

    private final DisplayNameLengthPolicy policy = new DisplayNameLengthPolicy();

    @Test
    void laengeWaechstAnDenGrenzen() {
        assertEquals(3, policy.lengthFor(1));
        assertEquals(3, policy.lengthFor(2195));
        assertEquals(4, policy.lengthFor(2196));
        assertEquals(4, policy.lengthFor(61465));
        assertEquals(5, policy.lengthFor(61466));
    }

    @Test
    void namenEnthaltenNurErlaubteZeichen() {
        ConsonantCodeGenerator generator = new ConsonantCodeGenerator(new Random(42), policy);
        for (int n = 1; n <= 3000; n++) {
            String name = generator.generate(n);
            assertTrue(name.matches("[BCDFGHJKLMNPQRSTVWXZ2-9]{3,4}"), name);
        }
    }

    @Test
    void alphabetHat28ZeichenOhneVokaleUndVerwechselbareZeichen() {
        assertEquals(28, ConsonantCodeGenerator.ALPHABET.length());
        for (char forbidden : "AEIOUY01".toCharArray()) {
            assertEquals(-1, ConsonantCodeGenerator.ALPHABET.indexOf(forbidden));
        }
        Set<Character> unique = new HashSet<>();
        for (char c : ConsonantCodeGenerator.ALPHABET.toCharArray()) {
            unique.add(c);
        }
        assertEquals(28, unique.size());
    }
}
