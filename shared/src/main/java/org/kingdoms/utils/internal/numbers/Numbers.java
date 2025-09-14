package org.kingdoms.utils.internal.numbers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Numbers {
    private static final DecimalFormat
            // CURRENCY_FORMAT = new DecimalFormat("#,###.##", new DecimalFormatSymbols(Locale.ENGLISH)),
            // CURRENCY_DEC_3_FORMAT = new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.ENGLISH)),
            // CURRENCY_DEC_4_FORMAT = new DecimalFormat("0.####", new DecimalFormatSymbols(Locale.ENGLISH)),
            SCIENTIFIC_FORMAT = new DecimalFormat("00E0", new DecimalFormatSymbols(Locale.ENGLISH));

    private Numbers() {}

    public static boolean isEnglishDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    public static String toOrdinalNumeral(int num) {
        if (num <= 0) throw new IllegalArgumentException("Ordinal numerals must start from 1");
        String str = Integer.toString(num);
        char lastDigit = str.charAt(str.length() - 1);

        if (lastDigit == '1') return str + "st";
        if (lastDigit == '2') return str + "nd";
        if (lastDigit == '3') return str + "rd";
        return str + "th"; // Even 100th
    }

    /**
     * Checks if the given string contains a number.
     *
     * @param str the string to check the numbers.
     * @return true if the string contains any number, otherwise false.
     * @since 1.0.0
     */
    public static boolean containsNumber(@Nullable CharSequence str) {
        if (str == null) return false;
        int len = str.length();
        if (len == 0) return false;

        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            if (isEnglishDigit(ch)) return true;
        }
        return false;
    }

    public static boolean containsAnyLangNumber(@Nullable CharSequence str) {
        if (str == null) return false;
        int len = str.length();
        if (len == 0) return false;

        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            if (Character.isDigit(ch)) return true;
        }
        return false;
    }

    public static boolean isNumeric(@Nullable String str) {
        if (str == null) return false;
        int len = str.length();
        if (len == 0) return false;

        int i = 0;
        if (len != 1) {
            char first = str.charAt(0);
            if (first == '-' || first == '+') i = 1;
        }

        while (i < len) {
            if (!isEnglishDigit(str.charAt(i++))) return false;
        }
        return true;
    }

    public static boolean isPureNumber(@Nullable String str) {
        if (com.google.common.base.Strings.isNullOrEmpty(str)) return false;
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            if (!isEnglishDigit(ch)) return false;
        }
        return true;
    }

    public static int getPositionOfFirstNonZeroDecimal(double number) {
        if (number >= 1.0) throw new IllegalArgumentException("Number is greater than 1.0");
        return (int) Math.abs(Math.floor(Math.log10(Math.abs(number))));
    }

    @Nonnull
    public static String toFancyNumber(double number) {
        if (number == 0.0) return "0";

        int decimalsToKeep;
        boolean positive = number >= 0;

        if (positive ? number >= 0.01 : number <= -0.01) {
            decimalsToKeep = 2;
        } else if (positive ? number >= 0.001 : number <= -0.001) {
            decimalsToKeep = 3;
        } else if (positive ? number >= 0.0001 : number <= -0.0001) {
            decimalsToKeep = 4;
        } else {
            return SCIENTIFIC_FORMAT.format(number);
        }

        return FancyNumberFactory.format(number, decimalsToKeep);
    }
}
