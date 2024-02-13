package org.kingdoms.constants.namespace;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.dataflow.qual.Pure;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * A container that namespaces values for registration to avoid conflicts.
 * These values are usually registered inside of a {@link NamespaceRegistry}
 * <p>
 * Extending this class is usually not a good idea. The {@link NamespaceContainer} should be used instead.
 */
@SuppressWarnings("PatternValidation")
public class Namespace {
    private final @NonNull String namespace, key;

    // We shouldn't allow dashes because of possible math equations and config option name translations.
    private static final String ACCEPTED_KEYS = "[A-Z0-9_]{3,100}";
    private static final String ACCEPTED_NAMESPACES = "[A-Za-z]{3,20}";
    private static final Pattern ACCEPTED_KEYS_PATTERN = Pattern.compile(ACCEPTED_KEYS);
    private static final Pattern ACCEPTED_NAMESPACES_PATTERN = Pattern.compile(ACCEPTED_NAMESPACES);

    public static final String KINGDOMS = "kingdoms";
    private static final char SEPARATOR = ':';

    /**
     * @throws IllegalArgumentException if either the namespace of the key don't match the standards.
     */
    @Pure
    public Namespace(@NonNull @org.intellij.lang.annotations.Pattern(ACCEPTED_NAMESPACES) String namespace,
                     @NonNull @org.intellij.lang.annotations.Pattern(ACCEPTED_KEYS) String key) {
        if (namespace == null || !ACCEPTED_NAMESPACES_PATTERN.matcher(namespace).matches())
            throw new IllegalStateException("Namespace string '" + namespace + "' doesn't match: " + ACCEPTED_NAMESPACES);
        if (key == null || !isValidKey(key))
            throw new IllegalStateException("Key string '" + key + "' doesn't match: " + ACCEPTED_KEYS);

        this.namespace = namespace;
        this.key = key;
    }

    @Pure
    public static boolean isValidKey(String key) {
        return ACCEPTED_KEYS_PATTERN.matcher(key).matches();
    }

    /**
     * @return returns a {@link #asNormalizedString()} except that the key is replaced with all lowercase letters and underscores replaced with dashes.
     */
    @NonNull
    @Pure
    public String getConfigOptionName() {
        String keyConfig = configOption(key);
        if (isKingdoms()) return keyConfig;
        return namespace + SEPARATOR + keyConfig;
    }

    /**
     * This is not intended for data usage, you should use {@link #asNormalizedString()} instead if you're saving data.
     *
     * @return {@link #getNamespace()} + ':' + {@link #getKey()}
     */
    @NonNull
    @Pure
    public final String asString() {
        return namespace + SEPARATOR + key;
    }

    /**
     * If this is a kingdoms name spaces, gives back the {@link #getKey()}
     * otherwise, it gives back {@link #asString()}
     */
    @NonNull
    @Pure
    public final String asNormalizedString() {
        if (isKingdoms()) return key;
        return asString();
    }

    /**
     * This method is only intended to be used by Kingdoms only.
     */
    @NonNull
    @Pure
    public static Namespace kingdoms(@NonNull @org.intellij.lang.annotations.Pattern(ACCEPTED_KEYS) String key) {
        return new Namespace(KINGDOMS, key);
    }

    /**
     * @return true if {@link #namespace} is equal to {@link #KINGDOMS}
     */
    public boolean isKingdoms() {
        return KINGDOMS.equals(this.namespace);
    }

    @Pure
    public final @NonNull String getNamespace() {
        return namespace;
    }

    @Pure
    public final @NonNull String getKey() {
        return key;
    }

    /**
     * Parses a string with the format of {@link #asNormalizedString()}
     */
    @NonNull
    @Pure
    public static Namespace fromString(@NonNull String str) {
        int separator = str.indexOf(SEPARATOR);
        if (separator == -1) return kingdoms(str);

        String namespace = str.substring(0, separator);
        String key = str.substring(separator + 1);

        return new Namespace(namespace, key);
    }

    @Pure
    public static Namespace fromConfigString(@NonNull String str) {
        int separator = str.indexOf(SEPARATOR);
        if (separator == -1) return kingdoms(configOptionToEnum(str));

        String namespace = str.substring(0, separator);
        String key = str.substring(separator + 1);

        return new Namespace(namespace, configOptionToEnum(key));
    }

    @Override
    @Pure
    public String toString() {
        return getClass().getSimpleName() + '[' + asString() + ']';
    }

    @Override
    @Pure
    public final boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Namespace)) return false;
        Namespace other = (Namespace) obj;
        return this.namespace.equals(other.namespace) && this.key.equals(other.key);
    }

    @Override
    @Pure
    public final int hashCode() {
        int hash = 5;
        hash = 47 * hash + this.namespace.hashCode();
        hash = 47 * hash + this.key.hashCode();
        return hash;
    }

    private static String configOptionToEnum(String str) {
        char[] chars = str.toCharArray();
        int len = str.length();

        for (int i = 0; i < len; i++) {
            char ch = chars[i];
            if (ch == '-') chars[i] = '_';
            else chars[i] = ((char) (ch & 0x5f));
        }
        return new String(chars);
    }

    private static String configOption(String str) {
        char[] chars = str.toCharArray();
        int len = str.length();

        for (int i = 0; i < len; i++) {
            char ch = chars[i];
            if (ch == '_') chars[i] = '-';
            else chars[i] = ((char) (ch | 0x20));
        }
        return new String(chars);
    }
}
