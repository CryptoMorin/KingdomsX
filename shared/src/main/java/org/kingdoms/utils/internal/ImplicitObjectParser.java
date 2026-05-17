package org.kingdoms.utils.internal;

import org.kingdoms.utils.internal.numbers.AnyNumber;
import org.kingdoms.utils.internal.numbers.NumberType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ImplicitObjectParser<T> {
    private static final Map<Class<?>, Function<String, Object>> STRING_PARSER = new HashMap<>();

    // @formatter:off
    public static class ImplicitObject<O> {
        public final O value;
        public ImplicitObject(O value) {this.value = value;}
    }

    public static final class ImplicitBasicObject extends ImplicitObject<Object> {
        public ImplicitBasicObject(Object value) {super(value);}
    }
    public static final class ImplicitCollection<T> extends ImplicitObject<Collection<T>> {
        public ImplicitCollection(Collection<T> value) {super(value);}
    }
    public static final class ImplicitMap<T> extends ImplicitObject<Map<String, T>> {
        public ImplicitMap(Map<String, T> value) {super(value);}
    }
    // @formatter:on

    public interface SimpleImplicitObjectParser<T> {
        ImplicitObject<?> parse(T object);
    }

    static {
        STRING_PARSER.put(String.class, s -> s);
        STRING_PARSER.put(Character.class, s -> s.charAt(0));

        register(boolean.class, Boolean::parseBoolean);

        register(byte.class, Byte::parseByte);
        register(short.class, Short::parseShort);
        register(int.class, Integer::parseInt);
        register(long.class, Long::parseLong);
        register(float.class, Float::parseFloat);
        register(double.class, Double::parseDouble);
    }

    private static void register(Class<?> clazz, Function<String, Object> parser) {
        STRING_PARSER.put(clazz, parser);
        STRING_PARSER.put(TypeUtils.toWrapper(clazz), parser);
    }

    private final SimpleImplicitObjectParser<T> parser;

    public ImplicitObjectParser(SimpleImplicitObjectParser<T> parser) {this.parser = parser;}

    @SuppressWarnings({"unchecked", "RedundantCast"})
    public Object parse(T object) {
        ImplicitObject<?> impliedObj = parser.parse(object);
        if (impliedObj instanceof ImplicitBasicObject) return impliedObj.value;
        if (impliedObj instanceof ImplicitCollection) {
            Collection<T> col = ((ImplicitCollection<T>) impliedObj).value;
            Collection<Object> items = col.stream().map(this::parse).collect(Collectors.toList());

            Class<?> prominentClass = findUniformCollectionType(items);
            if (prominentClass == Object.class) {
                return items;
            }

            Function<String, Object> parser = STRING_PARSER.get(prominentClass);
            return items.stream().map(x -> parser.apply(x.toString())).collect(Collectors.toList());
        }
        if (impliedObj instanceof ImplicitMap) {
            Map<String, T> map = ((ImplicitMap<T>) impliedObj).value;
            Map<String, Object> parsedMap = new HashMap<>(map.size());

            for (Map.Entry<String, T> entry : map.entrySet()) {
                parsedMap.put(entry.getKey(), parse(entry.getValue()));
            }

            return parsedMap;
        }

        throw new IllegalStateException("Unknown implicit object " + impliedObj + " for " + object);
    }

    public static Object parse(String string) {
        if (string == null) return null;
        if (string.isEmpty()) return "";

        if (string.equalsIgnoreCase("true")) return true;
        if (string.equalsIgnoreCase("false")) return false;

        AnyNumber num = AnyNumber.of(string);
        if (num != null) return num;

        if (string.length() == 1) return string.charAt(0);

        return string;
    }

    private Class<?> findUniformCollectionType(Collection<?> items) {
        Class<?> prominentClass = null;

        for (Object item : items) {
            Class<?> type;

            if (item instanceof CharSequence) {
                Object parsed = parse(item.toString());
                if (parsed != null) type = parsed.getClass();
                else continue; // Null type
            } else {
                type = item.getClass();
                if (prominentClass != type && !TypeUtils.isBasicObject(type)) {
                    return Object.class;
                }
            }

            if (type == String.class) return String.class;
            else if (prominentClass == null) prominentClass = type;
            else if (Number.class.isAssignableFrom(prominentClass) && Number.class.isAssignableFrom(type)) {
                if (prominentClass == Long.class && type == Float.class) prominentClass = Double.class;
                else if (NumberType.of(type).canSupport(NumberType.of(prominentClass))) prominentClass = type;
            } else if (prominentClass != type) {
                return String.class;
            }
        }

        return prominentClass;
    }
}
