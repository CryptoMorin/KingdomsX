package org.kingdoms.utils.internal.string;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Different ways to filter strings. A matcher doesn't need to necessarily search
 * and check the entire string to match, it can only check certain regions as well.
 * <p>
 * The main purpose of this class is to configurable values easier to manage and also
 * avoid the performance penalty of RegEx.
 */
public interface StringMatcher {
    boolean matches(@NotNull String string);

    @NotNull
    String asString();

    default Collection<StringMatcher> unwrap() {
        if (this instanceof Aggregate) {
            return Arrays.asList(((Aggregate) this).matchers);
        } else {
            return Collections.singletonList(this);
        }
    }

    final class Constant implements StringMatcher {
        private static final Constant TRUE = new Constant(true);
        private static final Constant FALSE = new Constant(false);
        private final boolean constant;

        private Constant(boolean constant) {this.constant = constant;}

        @Override
        public boolean matches(String string) {
            return constant;
        }

        @Override
        public String asString() {
            return "CONSTANT::" + constant;
        }
    }

    final class Exact implements StringMatcher {
        private final String exact;

        public Exact(String exact) {this.exact = exact;}

        @Override
        public boolean matches(String string) {
            return exact.equals(string);
        }

        @Override
        public String asString() {
            return exact;
        }
    }

    final class ExactCaseInsensitive implements StringMatcher {
        private final String exact;

        public ExactCaseInsensitive(String exact) {this.exact = exact;}

        @Override
        public boolean matches(String string) {
            return exact.equalsIgnoreCase(string);
        }

        @Override
        public String asString() {
            return "CI:" + exact;
        }
    }

    final class Contains implements StringMatcher {
        private final String contains;

        public Contains(String contains) {this.contains = contains;}

        @Override
        public boolean matches(String string) {
            return string.contains(contains);
        }

        @Override
        public String asString() {
            return "CONTAINS:" + contains;
        }
    }

    final class EndsWith implements StringMatcher {
        private final String endsWith;

        public EndsWith(String endsWith) {this.endsWith = endsWith;}

        @Override
        public boolean matches(String string) {
            return string.endsWith(endsWith);
        }

        @Override
        public String asString() {
            return "ENDS:" + endsWith;
        }
    }

    final class StartsWith implements StringMatcher {
        private final String startsWith;

        public StartsWith(String startsWith) {this.startsWith = startsWith;}

        @Override
        public boolean matches(String string) {
            return string.startsWith(startsWith);
        }

        @Override
        public String asString() {
            return "STARTS:" + startsWith;
        }
    }

    final class Regex implements StringMatcher {
        private final Pattern pattern;

        public Regex(Pattern pattern) {this.pattern = pattern;}

        @Override
        public boolean matches(String string) {
            return pattern.matcher(string).matches();
        }

        @Override
        public String asString() {
            return "REGEX" + (((pattern.flags() & Pattern.CASE_INSENSITIVE) != 0) ? "-CI" : "") + ':' + pattern.pattern();
        }
    }

    final class Hashed implements StringMatcher {
        private final Set<String> hashed;

        public Hashed(Set<String> hashed) {this.hashed = hashed;}

        @Override
        public boolean matches(String string) {
            return hashed.contains(string);
        }

        @Override
        public String asString() {
            return "Hashed:" + hashed;
        }
    }

    final class HashedCaseInsensitive implements StringMatcher {
        private final Set<String> hashed;

        public HashedCaseInsensitive(Set<String> hashed) {this.hashed = hashed;}

        @Override
        public boolean matches(String string) {
            return hashed.contains(string.toLowerCase(Locale.ENGLISH));
        }

        @Override
        public String asString() {
            return "HashedCaseInsensitive:" + hashed;
        }
    }

    final class Aggregate implements StringMatcher {
        private final StringMatcher[] matchers;

        public Aggregate(StringMatcher[] matchers) {this.matchers = matchers;}

        private static StringMatcher aggregate(Collection<StringMatcher> matchers) {
            int size = matchers.size();
            if (size == 0) return Constant.FALSE;
            if (size == 1) return matchers.iterator().next();
            return new Aggregate(matchers.toArray(new StringMatcher[0]));
        }

        @Override
        public boolean matches(String string) {
            for (StringMatcher matcher : matchers) {
                if (matcher.matches(string)) return true;
            }
            return false;
        }

        @Override
        public String asString() {
            return "Aggregate:" + Arrays.stream(matchers).map(StringMatcher::asString).collect(Collectors.toList());
        }
    }

    static StringMatcher optimize(Collection<StringMatcher> matchers) {
        int size = matchers.size();
        if (size == 0) return Constant.FALSE;
        if (size == 1) return matchers.iterator().next();

        List<StringMatcher> finalized = new ArrayList<>();

        // Check constants
        for (StringMatcher matcher : matchers) {
            finalized.add(matcher);
            if (matcher instanceof Constant) break;
        }

        { // Optimize exact matches into a HashSet
            int countExact = (int) finalized.stream().filter(x -> x instanceof Exact).count();
            if (countExact > 3) {
                // The order might improve performance
                int index = 0;
                int startIndex = -1;

                Set<String> hashed = new HashSet<>(countExact);
                Iterator<StringMatcher> iter = finalized.iterator();
                while (iter.hasNext()) {
                    StringMatcher matcher = iter.next();
                    if (matcher instanceof Exact) {
                        iter.remove();
                        startIndex = index;
                        hashed.add(((Exact) matcher).exact);
                    }
                    index++;
                }

                finalized.add(startIndex, new Hashed(hashed));
            }
        }

        { // Optimize case-insensitive exact matches into a HashSet
            int countExact = (int) finalized.stream().filter(x -> x instanceof ExactCaseInsensitive).count();
            if (countExact > 3) {
                // The order might improve performance
                int index = 0;
                int startIndex = -1;

                Set<String> hashed = new HashSet<>(countExact);
                Iterator<StringMatcher> iter = finalized.iterator();
                while (iter.hasNext()) {
                    StringMatcher matcher = iter.next();
                    if (matcher instanceof ExactCaseInsensitive) {
                        iter.remove();
                        startIndex = index;
                        hashed.add(((ExactCaseInsensitive) matcher).exact.toLowerCase(Locale.ENGLISH));
                    }
                    index++;
                }

                finalized.add(startIndex, new HashedCaseInsensitive(hashed));
            }
        }

        return Aggregate.aggregate(finalized);
    }

    @NotNull
    static StringMatcher group(@NotNull Collection<StringMatcher> matchers) {
        return optimize(matchers);
    }

    @NotNull
    static StringMatcher parseAndGroup(@NotNull Collection<String> matchers) {
        return optimize(matchers.stream().map(StringMatcher::fromString).collect(Collectors.toList()));
    }

    @NotNull
    static Collection<StringMatcher> parse(@NotNull Collection<String> matchers) {
        return matchers.stream().map(StringMatcher::fromString).collect(Collectors.toList());
    }

    StringMatcher ALWAYS_FALSE = Constant.FALSE;
    StringMatcher ALWAYS_TRUE = Constant.TRUE;

    @NotNull
    static StringMatcher fromString(String text) {
        Objects.requireNonNull(text, "Cannot construct checker from null text");

        if (text.equals("*")) return Constant.TRUE;

        int handlerIndexEnd = text.indexOf(':');
        if (handlerIndexEnd == -1) return new Exact(text);

        // TODO Add Negation for StringMatcher
        //      if (text.charAt(0) == '!')
        String handlerName = text.substring(0, handlerIndexEnd);
        String realText = text.substring(handlerIndexEnd + 1);

        // TODO Add the "@CI" option for all of them.
        // @formatter:off
        switch (handlerName) {
            case "CI"       : return new ExactCaseInsensitive(realText);
            case "CONTAINS" : return new Contains  (realText);
            case "STARTS"   : return new StartsWith(realText);
            case "ENDS"     : return new EndsWith  (realText);
            case "REGEX"    : return new Regex     (Pattern.compile(realText));
            case "REGEX@CI" : return new Regex     (Pattern.compile(realText, Pattern.CASE_INSENSITIVE));
        }
        // @formatter:on

        return new Exact(text);
    }
}
