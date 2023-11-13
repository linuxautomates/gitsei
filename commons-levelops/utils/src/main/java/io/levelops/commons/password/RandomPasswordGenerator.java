package io.levelops.commons.password;

import java.security.SecureRandom;

public class RandomPasswordGenerator {
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = UPPER.toLowerCase();
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARS = "$^&@#+_";
    private static final String ALPHANUMS = UPPER + SPECIAL_CHARS + LOWER + DIGITS;

    private static final SecureRandom RANDOM_GEN = new SecureRandom();
    private static final char[] SYMBOLS = ALPHANUMS.toCharArray();
    private static final int DEFAULT_LENGTH = 50;

    //Secure random string generator
    public static String nextString() {
        char[] buf = new char[DEFAULT_LENGTH];
        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = SYMBOLS[RANDOM_GEN.nextInt(SYMBOLS.length)];
        return new String(buf);
    }

    //Secure random string generator with specified length
    public static String nextString(int length) {
        char[] buf = new char[length];
        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = SYMBOLS[RANDOM_GEN.nextInt(SYMBOLS.length)];
        return new String(buf);
    }
}