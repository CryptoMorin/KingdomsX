package org.kingdoms.utils.internal.numbers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

class FancyNumberFactory {
    private static final double LONG_MAX_D = (double) Long.MAX_VALUE;

    public static String format(double value, int maxDecimals) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        if (maxDecimals < 0) maxDecimals = 0;

        boolean negative = value < 0;
        double abs = Math.abs(value);

        // Fast/safe path constraints:
        //  - integer part fits in long
        //  - and we can build scale as a long (maxDecimals <= 18)

        if (maxDecimals <= 18 && abs <= LONG_MAX_D) {
            return formatLong(value, negative, abs, maxDecimals);
        } else {
            // fallback for very large numbers or when we cannot safely use long-based scaling
            return formatWithBigDecimal(value, maxDecimals, negative);
        }
    }

    private static String formatLong(double value, boolean negative, double abs, int maxDecimals) {
        // Fast/safe path constraints:
        //  - integer part fits in long
        //  - and we can build scale as a long (maxDecimals <= 18)
        // safe to work with long arithmetic & scale as long
        long integerPart = (long) abs;
        double fractional = abs - integerPart;

        long scale = 1L;
        for (int i = 0; i < maxDecimals; i++) scale *= 10L;

        long fracScaled = 0L;
        if (maxDecimals > 0 && fractional > 0.0) {
            // rounding
            fracScaled = Math.round(fractional * scale);
            if (fracScaled >= scale) {
                // carry into integer part
                // if integerPart == Long.MAX_VALUE we cannot increment safely => fall back
                if (integerPart == Long.MAX_VALUE) {
                    return formatWithBigDecimal(value, maxDecimals, negative);
                }
                fracScaled = 0L;
                integerPart++;
            }
        }

        // compute digit counts
        int intDigits = digitCount(integerPart);
        int commas = (intDigits - 1) / 3;

        int fracChars = 0;
        long fracWrite = 0L;
        int leadingZeros = 0;
        if (maxDecimals > 0 && fracScaled > 0) {
            // remember original length to compute leading zeros (padded to maxDecimals)
            int origLen = digitCount(fracScaled);
            leadingZeros = maxDecimals - origLen;

            // trim trailing zeros numerically
            long trimmed = fracScaled;
            while (trimmed % 10L == 0L) trimmed /= 10L;

            int trimmedLen = digitCount(trimmed);
            fracChars = 1 + leadingZeros + trimmedLen; // '.' + leading zeros + digits
            fracWrite = trimmed; // write this LSB-first
        }

        int capacity = (negative ? 1 : 0) + intDigits + commas + fracChars;
        byte[] buf = new byte[capacity];
        int pos = capacity;

        // write fractional digits (if any): write LSB-first, then leading zeros, then dot
        if (fracChars > 0) {
            // write trimmed digits LSB-first
            long tmp = fracWrite;
            while (tmp > 0) {
                buf[--pos] = (byte) ('0' + (int) (tmp % 10));
                tmp /= 10;
            }
            // write leading zeros (these come before digits)
            for (int i = 0; i < leadingZeros; i++) {
                buf[--pos] = '0';
            }
            // write decimal point
            buf[--pos] = '.';
        }

        // write integer part with commas, LSB-first grouping
        int groupCount = 0;
        if (integerPart == 0) {
            buf[--pos] = '0';
        } else {
            long tmp = integerPart;
            while (tmp > 0) {
                if (groupCount == 3) {
                    buf[--pos] = ',';
                    groupCount = 0;
                }
                buf[--pos] = (byte) ('0' + (int) (tmp % 10));
                tmp /= 10;
                groupCount++;
            }
        }

        // write sign
        if (negative) buf[--pos] = '-';

        // build final string
        return new String(buf, pos, capacity - pos, StandardCharsets.US_ASCII);
    }

    // fallback using BigDecimal for full-precision & extremely large values
    private static String formatWithBigDecimal(double value, int maxDecimals, boolean negative) {
        double abs = Math.abs(value);
        BigDecimal bd = BigDecimal.valueOf(abs).setScale(maxDecimals, RoundingMode.HALF_UP);
        String plain = bd.toPlainString(); // no exponent form

        // split integer / fraction
        int dot = plain.indexOf('.');
        String intPart = (dot >= 0) ? plain.substring(0, dot) : plain;
        String fracPart = (dot >= 0) ? plain.substring(dot + 1) : "";

        // trim trailing zeros from fraction (to match original behavior)
        if (!fracPart.isEmpty()) {
            int end = fracPart.length();
            while (end > 0 && fracPart.charAt(end - 1) == '0') end--;
            fracPart = fracPart.substring(0, end);
        }

        // insert commas into integer part
        int intLen = intPart.length();
        int commas = (intLen - 1) / 3;
        int firstGroup = (intLen % 3 == 0) ? 3 : (intLen % 3);
        if (intLen <= 3) firstGroup = intLen;

        int totalLen = (negative ? 1 : 0) + intLen + commas + (fracPart.isEmpty() ? 0 : 1 + fracPart.length());
        byte[] buf = new byte[totalLen];
        int idx = 0;

        if (negative) buf[idx++] = '-';

        // copy first group
        buf[idx++] = (byte) intPart.charAt(0);
        for (int i = 1; i < firstGroup; i++) buf[idx++] = (byte) intPart.charAt(i);

        // copy remaining groups with commas
        for (int i = firstGroup; i < intLen; i += 3) {
            buf[idx++] = ',';
            buf[idx++] = (byte) intPart.charAt(i);
            buf[idx++] = (byte) intPart.charAt(i + 1);
            buf[idx++] = (byte) intPart.charAt(i + 2);
        }

        // fractional
        if (!fracPart.isEmpty()) {
            buf[idx++] = '.';
            for (int i = 0; i < fracPart.length(); i++) {
                buf[idx++] = (byte) fracPart.charAt(i);
            }
        }

        return new String(buf, 0, idx, StandardCharsets.US_ASCII);
    }

    /**
     * divide-and-conquer method from <a href="https://stackoverflow.com/a/1308407">an StackOverflow answer</a>.
     */
    private static int digitCount(long v) {
        if (v < 0) v = -v; // handle negatives

        if (v < 100000000L) {
            if (v < 10000L) {
                if (v < 100L) {
                    return v < 10L ? 1 : 2;
                } else {
                    return v < 1000L ? 3 : 4;
                }
            } else {
                if (v < 1_000_000L) {
                    return v < 100_000L ? 5 : 6;
                } else {
                    return v < 10_000_000L ? 7 : 8;
                }
            }
        } else {
            if (v < 10_000_000_000_000L) {
                if (v < 1_000_000_000L) {
                    return 9;
                } else {
                    return v < 100_000_000_000L ? 11 : 12;
                }
            } else {
                if (v < 100_000_000_000_000L) return 14;
                if (v < 1_000_000_000_000_000L) return 15;
                if (v < 10_000_000_000_000_000L) return 16;
                if (v < 100_000_000_000_000_000L) return 17;
                if (v < 1_000_000_000_000_000_000L) return 18;
                return 19;
            }
        }
    }
}
