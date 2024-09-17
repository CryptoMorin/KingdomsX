/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Jon Chambers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.kingdoms.utils.internal.uuid;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

/**
 * A utility class for quickly and efficiently parsing {@link java.util.UUID} instances from strings and writing UUID
 * instances as strings. The methods contained in this class are optimized for speed and to minimize garbage collection
 * pressure. In benchmarks, {@link #fromString(CharSequence)} is a little more than 14 times faster than
 * {@link UUID#fromString(String)}, and {@link #toString(UUID)} is a little more than six times faster than
 * {@link UUID#toString()} when compared to the implementations in Java 8 and older. Under Java 9 and newer,
 * {@link #fromString(CharSequence)} is about six times faster than the JDK implementation and {@link #toString(UUID)}
 * does not offer any performance enhancements (or regressions!) the {@link UUID#toString()} will be faster.
 * <p>
 * Modified version by Crypto Morin
 *
 * @author <a href="https://github.com/jchambers/">Jon Chambers</a>
 * @version 2020.30
 */
public final class FastUUID {
    /**
     * OpenJDK 14 and newer use a fancy native approach to converting UUIDs to strings and we're better off using
     * that if it's available.
     * <p>
     * Java 11+ use Long.fastUUID which for some reasons is faster.
     */
    private static final boolean USE_JDK_UUID_TO_STRING;
    private static final int UUID_STRING_LENGTH = 36;
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final long[] NIBBLES = new long[128];
    public static final UUID ZERO = new UUID(0, 0);

    static {
        String java = System.getProperty("java.specification.version");
        int version;
        try {
            version = Integer.parseInt(java);
        } catch (NumberFormatException ex) {
            version = 0;
        }
        USE_JDK_UUID_TO_STRING = version >= 11;
    }

    static {
        Arrays.fill(NIBBLES, -1);

        NIBBLES['0'] = 0x0;
        NIBBLES['1'] = 0x1;
        NIBBLES['2'] = 0x2;
        NIBBLES['3'] = 0x3;
        NIBBLES['4'] = 0x4;
        NIBBLES['5'] = 0x5;
        NIBBLES['6'] = 0x6;
        NIBBLES['7'] = 0x7;
        NIBBLES['8'] = 0x8;
        NIBBLES['9'] = 0x9;

        NIBBLES['a'] = 0xa;
        NIBBLES['b'] = 0xb;
        NIBBLES['c'] = 0xc;
        NIBBLES['d'] = 0xd;
        NIBBLES['e'] = 0xe;
        NIBBLES['f'] = 0xf;

        NIBBLES['A'] = 0xa;
        NIBBLES['B'] = 0xb;
        NIBBLES['C'] = 0xc;
        NIBBLES['D'] = 0xd;
        NIBBLES['E'] = 0xe;
        NIBBLES['F'] = 0xf;
    }

    private FastUUID() {
    }

    /**
     * Static factory to retrieve a type 4 (pseudo randomly generated) UUID.
     * Copied from JDK15
     *
     * @param random the randomizer to use for generating the UUID.
     * @return A randomly generated {@code UUID}
     */
    public static UUID randomUUID(Random random) {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);

        randomBytes[6] &= 0x0f;  /* clear version        */
        randomBytes[6] |= 0x40;  /* set to version 4     */
        randomBytes[8] &= 0x3f;  /* clear variant        */
        randomBytes[8] |= 0x80;  /* set to IETF variant  */
        return bytesToUUID(randomBytes);
    }

    /**
     * Copied from JDK15 {@code UUID} private constructor
     * Anding ({@literal &}) an integer with 0xFF leaves only the least significant byte, aka masking
     * <p>
     * least significant bit (LSB): Example for a 32-bit int
     * 011101010101010101010101010110110
     * ^^^^^^^^
     * <p>
     * 0xff = 255 = 1 1 1 1 1 1 1 1
     * <p>
     * 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1
     * {@literal &}  0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1
     * -------------------------------
     * 0 0 0 0 0 0 0 0 0 1 0 1 0 1 0 1
     * {@literal ^}
     *
     * @param data a 16 long byte array to construct a {@link UUID}.
     * @return a {@link UUID} created by the bytes.
     */
    public static UUID bytesToUUID(byte[] data) {
        long msb = 0, lsb = 0;
        for (int i = 0; i < 8; i++) msb = (msb << 8) | (data[i] & 0xff);
        for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (data[i] & 0xff);
        return new UUID(msb, lsb);
    }

    /**
     * Avoids the null and {@link #getClass()} check of the standard
     * {@link UUID#equals(Object)} method for known objects.
     * <p>
     * Hopefully JVM will inline the method calls.
     */
    public static boolean equals(UUID first, UUID other) {
        return first.getMostSignificantBits() == other.getMostSignificantBits() &&
                first.getLeastSignificantBits() == other.getLeastSignificantBits();
    }

    /**
     * Parses a UUID from the given character sequence. The character sequence must represent a UUID as described in
     * {@link UUID#toString()}.
     *
     * @param uuid the character sequence from which to parse a UUID.
     * @return the UUID represented by the given character sequence.
     * @throws IllegalArgumentException if the given character sequence does not conform to the string representation as
     *                                  described in {@link UUID#toString()}
     */
    public static UUID fromString(CharSequence uuid) {
        try {
            long mostSignificantBits = (((((((((((((((getHexValueForChar(uuid.charAt(0)) << 60)
                    | getHexValueForChar(uuid.charAt(1)) << 56)
                    | getHexValueForChar(uuid.charAt(2)) << 52)
                    | getHexValueForChar(uuid.charAt(3)) << 48)
                    | getHexValueForChar(uuid.charAt(4)) << 44)
                    | getHexValueForChar(uuid.charAt(5)) << 40)
                    | getHexValueForChar(uuid.charAt(6)) << 36)
                    | getHexValueForChar(uuid.charAt(7)) << 32)

                    | getHexValueForChar(uuid.charAt(9)) << 28)
                    | getHexValueForChar(uuid.charAt(10)) << 24)
                    | getHexValueForChar(uuid.charAt(11)) << 20)
                    | getHexValueForChar(uuid.charAt(12)) << 16)

                    | getHexValueForChar(uuid.charAt(14)) << 12)
                    | getHexValueForChar(uuid.charAt(15)) << 8)
                    | getHexValueForChar(uuid.charAt(16)) << 4)
                    | getHexValueForChar(uuid.charAt(17));


            long leastSignificantBits = (((((((((((((((getHexValueForChar(uuid.charAt(19)) << 60)
                    | getHexValueForChar(uuid.charAt(20)) << 56)
                    | getHexValueForChar(uuid.charAt(21)) << 52)
                    | getHexValueForChar(uuid.charAt(22)) << 48)
                    | getHexValueForChar(uuid.charAt(24)) << 44)
                    | getHexValueForChar(uuid.charAt(25)) << 40)
                    | getHexValueForChar(uuid.charAt(26)) << 36)
                    | getHexValueForChar(uuid.charAt(27)) << 32)
                    | getHexValueForChar(uuid.charAt(28)) << 28)
                    | getHexValueForChar(uuid.charAt(29)) << 24)
                    | getHexValueForChar(uuid.charAt(30)) << 20)
                    | getHexValueForChar(uuid.charAt(31)) << 16)
                    | getHexValueForChar(uuid.charAt(32)) << 12)
                    | getHexValueForChar(uuid.charAt(33)) << 8)
                    | getHexValueForChar(uuid.charAt(34)) << 4)
                    | getHexValueForChar(uuid.charAt(35));

            return new UUID(mostSignificantBits, leastSignificantBits);
        } catch (Throwable throwable) {
            throw new MalformedUUIDException(uuid, throwable);
        }
    }

    /**
     * Returns a string representation of the given UUID. The returned string is formatted as described in
     * {@link UUID#toString()}.
     *
     * @param uuid the UUID to represent as a string
     * @return a string representation of the given UUID
     */
    public static String toString(UUID uuid) {
        if (USE_JDK_UUID_TO_STRING) return uuid.toString();

        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();
        char[] uuidChars = new char[UUID_STRING_LENGTH];

        uuidChars[0] = HEX_DIGITS[(int) ((mostSignificantBits & 0xf000000000000000L) >>> 60)];
        uuidChars[1] = HEX_DIGITS[(int) ((mostSignificantBits & 0x0f00000000000000L) >>> 56)];
        uuidChars[2] = HEX_DIGITS[(int) ((mostSignificantBits & 0x00f0000000000000L) >>> 52)];
        uuidChars[3] = HEX_DIGITS[(int) ((mostSignificantBits & 0x000f000000000000L) >>> 48)];
        uuidChars[4] = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000f00000000000L) >>> 44)];
        uuidChars[5] = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000f0000000000L) >>> 40)];
        uuidChars[6] = HEX_DIGITS[(int) ((mostSignificantBits & 0x000000f000000000L) >>> 36)];
        uuidChars[7] = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000000f00000000L) >>> 32)];
        uuidChars[8] = '-';
        uuidChars[9] = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000000f0000000L) >>> 28)];
        uuidChars[10] = HEX_DIGITS[(int) ((mostSignificantBits & 0x000000000f000000L) >>> 24)];
        uuidChars[11] = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000000000f00000L) >>> 20)];
        uuidChars[12] = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000000000f0000L) >>> 16)];
        uuidChars[13] = '-';
        uuidChars[14] = HEX_DIGITS[(int) ((mostSignificantBits & 0x000000000000f000L) >>> 12)];
        uuidChars[15] = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000000000000f00L) >>> 8)];
        uuidChars[16] = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000000000000f0L) >>> 4)];
        uuidChars[17] = HEX_DIGITS[(int) (mostSignificantBits & 0x000000000000000fL)];
        uuidChars[18] = '-';
        uuidChars[19] = HEX_DIGITS[(int) ((leastSignificantBits & 0xf000000000000000L) >>> 60)];
        uuidChars[20] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0f00000000000000L) >>> 56)];
        uuidChars[21] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00f0000000000000L) >>> 52)];
        uuidChars[22] = HEX_DIGITS[(int) ((leastSignificantBits & 0x000f000000000000L) >>> 48)];
        uuidChars[23] = '-';
        uuidChars[24] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000f00000000000L) >>> 44)];
        uuidChars[25] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000f0000000000L) >>> 40)];
        uuidChars[26] = HEX_DIGITS[(int) ((leastSignificantBits & 0x000000f000000000L) >>> 36)];
        uuidChars[27] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000000f00000000L) >>> 32)];
        uuidChars[28] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000000f0000000L) >>> 28)];
        uuidChars[29] = HEX_DIGITS[(int) ((leastSignificantBits & 0x000000000f000000L) >>> 24)];
        uuidChars[30] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000000000f00000L) >>> 20)];
        uuidChars[31] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000000000f0000L) >>> 16)];
        uuidChars[32] = HEX_DIGITS[(int) ((leastSignificantBits & 0x000000000000f000L) >>> 12)];
        uuidChars[33] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000000000000f00L) >>> 8)];
        uuidChars[34] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000000000000f0L) >>> 4)];
        uuidChars[35] = HEX_DIGITS[(int) (leastSignificantBits & 0x000000000000000fL)];

        return new String(uuidChars);
    }

    private static long getHexValueForChar(char ch) {
        try {
            long hex = NIBBLES[ch];
            if (hex == -1) throw new IllegalArgumentException("Illegal hexadecimal digit: " + ch);
            return hex;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("Illegal hexadecimal digit: " + ch);
        }
    }
}
