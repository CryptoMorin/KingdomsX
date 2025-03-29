package org.kingdoms.utils.internal.reflection;

import org.kingdoms.utils.internal.arrays.ArrayUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

/**
 * A class for reflecting on your behavior and morals of committing API evasion and using the dreaded NMS.
 */
public final class Reflect {
    /**
     * @param className to autocomplete the class name + package you can use Ctrl+Alt+Space for IntelliJ on Windows.
     */
    public static boolean classExists(String className) {
        try {
            // Prevent static initialization
            Class.forName(className, false, Reflect.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        } catch (Throwable e) {
            // Not sure if this'd happen, but some server software like silencing errors for some reasons.
            e.printStackTrace();
            return true;
        }
    }

    public static Field getDeclaredField(Class<?> clazz, String... names) throws NoSuchFieldException {
        NoSuchFieldException error = null;

        for (String name : names) {
            try {
                /* 1.20.6-36 The fuck??????
                 * Caused by: java.lang.NullPointerException: Cannot invoke "java.lang.Class.name()" because "clazz" is null
                 *         at io.papermc.paper.pluginremap.reflect.PaperReflection.mapDeclaredFieldName(PaperReflection.java:77) ~[paper-1.20.6.jar:git-Paper-36]
                 *         at io.papermc.reflectionrewriter.runtime.AbstractDefaultRulesReflectionProxy.getDeclaredField(AbstractDefaultRulesReflectionProxy.java:90) ~[reflection-rewriter-runtime-0.0.1.jar:?]
                 *         at io.papermc.paper.pluginremap.reflect.PaperReflectionHolder.getDeclaredField(Unknown Source) ~[paper-1.20.6.jar:git-Paper-36]
                 *         at KingdomsX-1.16.20.5.jar/org.kingdoms.utils.internal.reflection.Reflect.getDeclaredField(Reflect.java:31) ~[KingdomsX-1.16.20.5.jar:?]
                 */
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException ex) {
                if (error == null)
                    error = new NoSuchFieldException("Couldn't find any of the fields " + Arrays.toString(names) + " in class: " + clazz);
                error.addSuppressed(ex);
            }
        }

        throw error;
    }

    public static Class<?>[] getClassHierarchy(Class<?> clazz, boolean allowAnonymous) {
        List<Class<?>> classes = new ArrayList<>();

        Class<?> lastUpperClass = clazz;
        while ((lastUpperClass = (allowAnonymous ? lastUpperClass.getEnclosingClass() : lastUpperClass.getDeclaringClass())) != null) {
            if (classes.isEmpty()) classes.add(clazz);
            classes.add(lastUpperClass);
        }

        if (classes.isEmpty()) return new Class<?>[]{clazz};

        Class<?>[] reversed = classes.toArray(new Class<?>[0]);
        return ArrayUtils.reverse(reversed);
    }

    public static List<Field> getFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> hierarchy : getClassHierarchy(clazz, false)) {
            fields.addAll(Arrays.asList(hierarchy.getDeclaredFields()));
        }
        return fields;
    }

    public static String toString(Object obj) {
        return toString(obj, false);
    }

    public static String toString(Object obj, boolean direct) {
        Class<?> clazz = obj.getClass();
        List<Field> fields = direct ? Arrays.asList(clazz.getDeclaredFields()) : getFields(clazz);
        StringBuilder string = new StringBuilder(clazz.getSimpleName()).append('{');
        StringJoiner joiner = new StringJoiner(", ");

        for (Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            try {
                joiner.add(field.getName() + '=' + field.get(obj));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return string.append(joiner).append('}').toString();
    }
}
