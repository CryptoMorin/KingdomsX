package org.kingdoms.utils.internal.string;

import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Different ways to filter strings. A matcher doesn't need to necessarily search
 * and check the entire string to match, it can only check certain regions as well.
 * <p>
 * The main purpose of this class is to configurable values easier to manage and also
 * avoid the performance penalty of RegEx.
 */
public interface StringMatcher {
    boolean matches(String string);

    final class Exact implements StringMatcher {
        private final String exact;

        public Exact(String exact) {this.exact = exact;}

        @Override
        public boolean matches(String string) {
            return exact.equals(string);
        }
    }

    final class Contains implements StringMatcher {
        private final String contains;

        public Contains(String contains) {this.contains = contains;}

        @Override
        public boolean matches(String string) {
            return string.contains(contains);
        }
    }

    final class EndsWith implements StringMatcher {
        private final String endsWith;

        public EndsWith(String endsWith) {this.endsWith = endsWith;}

        @Override
        public boolean matches(String string) {
            return string.endsWith(endsWith);
        }
    }

    final class StartsWith implements StringMatcher {
        private final String startsWith;

        public StartsWith(String startsWith) {this.startsWith = startsWith;}

        @Override
        public boolean matches(String string) {
            return string.endsWith(startsWith);
        }
    }

    final class Regex implements StringMatcher {
        private final Pattern pattern;

        public Regex(Pattern pattern) {this.pattern = pattern;}

        @Override
        public boolean matches(String string) {
            return pattern.matcher(string).matches();
        }
    }

    final class Aggregate implements StringMatcher {
        private final StringMatcher[] matchers;

        public Aggregate(StringMatcher[] matchers) {this.matchers = matchers;}

        @Override
        public boolean matches(String string) {
            for (StringMatcher matcher : matchers) {
                if (matcher.matches(string)) return true;
            }
            return false;
        }
    }

    static StringMatcher group(Collection<StringMatcher> matchers) {
        return new Aggregate(matchers.toArray(new StringMatcher[0]));
    }

    static StringMatcher fromString(String text) {
        Objects.requireNonNull(text, "Cannot construct checker from null text");
        int handlerIndexEnd = text.indexOf(':');
        if (handlerIndexEnd == -1) return new Exact(text);

        String handlerName = text.substring(0, handlerIndexEnd);
        String realText = text.substring(handlerIndexEnd + 1);

        // @formatter:off
        switch (handlerName) {
            case "CONTAINS": return new Contains  (realText);
            case "STARTS"  : return new StartsWith(realText);
            case "ENDS"    : return new EndsWith  (realText);
            case "REGEX"   : return new Regex     (Pattern.compile(realText));
        }
        // @formatter:on

        return new Exact(text);
    }
}
