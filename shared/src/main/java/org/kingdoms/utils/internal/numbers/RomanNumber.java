package org.kingdoms.utils.internal.numbers;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;

public final class RomanNumber {
    private RomanNumber() {}

    private static final int[] ROMAN_CHARS = new int[22];
    private static final String[]
            M = {"", "M", "MM", "MMM"},
            C = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"},
            X = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"},
            I = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};

    static {
        char start = 'C';
        ROMAN_CHARS['M' - start] = 1000;
        ROMAN_CHARS['D' - start] = 500;
        ROMAN_CHARS['C' - start] = 100;
        ROMAN_CHARS['L' - start] = 50;
        ROMAN_CHARS['X' - start] = 10;
        ROMAN_CHARS['V' - start] = 5;
        ROMAN_CHARS['I' - start] = 1;
    }

    @SuppressWarnings("ConstantConditions") // Because of IntRange annotation
    @NonNull
    public static String toRoman(@IntRange(from = 1, to = 3999) int num) {
        if (num <= 0 || num >= 4000) return String.valueOf(num);

        String thousands = M[num / 1000];
        String hundereds = C[(num % 1000) / 100];
        String tens = X[(num % 100) / 10];
        String ones = I[num % 10];

        return thousands + hundereds + tens + ones;
    }

    @IntRange(from = 0, to = 1000)
    private static int value(char chr) {
        int index = chr - 'C';
        return index < 0 || index > ROMAN_CHARS.length ? 0 : ROMAN_CHARS[index];
    }

    @IntRange(from = 0, to = 3999)
    public static int fromRoman(@Nullable CharSequence roman) {
        if (roman == null) return 0;
        int len = roman.length();
        if (len == 0) return 0;

        len--;
        int result = 0;

        for (int i = 0; i < len; i++) {
            int first = value(roman.charAt(i));
            if (first == 0) return 0;

            if (first >= value(roman.charAt(i + 1))) result += first;
            else result -= first;
        }

        int last = value(roman.charAt(len));
        return last == 0 ? 0 : result + last;
    }
}
