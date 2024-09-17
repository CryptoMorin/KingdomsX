package org.kingdoms.utils.internal.string;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;

public class QuantumString implements CharSequence, Cloneable {
    private final @Nullable String original;
    private final @NonNull String quantumValue;

    public static QuantumString of(String original) {
        return new QuantumString(original, true);
    }

    /**
     * @param original the string to wrap.
     * @param quantum  if this wrapper should be case-insensitive, otherwise case-sensitive.
     */
    public QuantumString(@NonNull String original, boolean quantum) {
        Objects.requireNonNull(original, "Quantum original string cannot be null");
        this.original = quantum ? original : null;

        // The local doesn't matter as long as it's consistent.
        this.quantumValue = quantum ? original.toLowerCase(Locale.ENGLISH) : original;
    }

    public static QuantumString empty() {
        return new QuantumString("", false);
    }

    public boolean isQuantum() {
        return original != null;
    }

    @Override
    public int hashCode() {
        return quantumValue.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj ||
                (obj instanceof QuantumString && this.quantumValue.equals(((QuantumString) obj).quantumValue));
    }

    @Override
    public String toString() {
        return "QuantumString:[quantum= " + isQuantum() + ", original=" + original + ", quantumValue=" + quantumValue + ']';
    }

    @Override
    public int length() {
        return quantumValue.length();
    }

    @SuppressWarnings("all")
    public boolean isEmpty() {
        return quantumValue.isEmpty();
    }

    @Override
    public char charAt(int index) {
        return getQuantum().charAt(index);
    }

    @NotNull
    @Override
    public CharSequence subSequence(int start, int end) {
        return getQuantum().subSequence(start, end);
    }

    @NotNull
    @Override
    public IntStream chars() {
        return getQuantum().chars();
    }

    @NotNull
    @Override
    public IntStream codePoints() {
        return getQuantum().codePoints();
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Nullable
    public String getOriginal() {
        return original;
    }

    @NonNull
    public String getQuantumValue() {
        return quantumValue;
    }

    @NonNull
    public String getQuantum() {
        return isQuantum() ? original : quantumValue;
    }
}
