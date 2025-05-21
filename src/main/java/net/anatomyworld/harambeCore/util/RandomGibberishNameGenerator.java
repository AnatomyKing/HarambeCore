package net.anatomyworld.harambeCore.util;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Generates a fantasy/sci-fi-style gibberish name by stitching syllables.
 * Results are lowercase and alphanumeric, 3–4 syllables long.
 */
public final class RandomGibberishNameGenerator {

    private static final SecureRandom RNG = new SecureRandom();

    // Expanded consonant-vowel patterns for more variety
    private static final String[] SYLLABLES = {
            "ka", "ke", "ki", "ko", "ku",
            "ra", "re", "ri", "ro", "ru",
            "za", "ze", "zi", "zo", "zu",
            "va", "ve", "vi", "vo", "vu",
            "ta", "te", "ti", "to", "tu",
            "na", "ne", "ni", "no", "nu",
            "sa", "se", "si", "so", "su",
            "xa", "xe", "xi", "xo", "xu",
            "lo", "la", "li", "le", "lu",
            "mo", "ma", "mi", "me", "mu",
            "jo", "ja", "ji", "je", "ju",
            "qo", "qa", "qi", "qu", "qe"
    };

    private RandomGibberishNameGenerator() {
        // util-class – no instantiation
    }

    public static String generate() {
        int parts = 3 + RNG.nextInt(2); // 3 or 4 syllables

        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < parts; i++) {
            sb.append(SYLLABLES[RNG.nextInt(SYLLABLES.length)]);
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }
}
