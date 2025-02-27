package org.kingdoms.utils;

import com.cryptomorin.xseries.reflection.XAccessFlag;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * A class that can change the logging level of different kinds of loggers.
 */
public final class LoggerLevelInjector {
    public static void setLevel(Object logger, Level level) {
        // Unfortunately at the bottom of Log4jLogger delegates
        // resides org.apache.logging.log4j.core.Logger which uses the inner class
        // PrivateConfig with an int log level that is final, therefore we cannot change it...

        Class<?> clazz = logger.getClass();

        while (true) {
            Optional<Field> delegate = Arrays.stream(clazz.getDeclaredFields())
                    .filter(x -> x.getName().equals("logger"))
                    .filter(x -> x.getType().getSimpleName().contains("Logger"))
                    .findFirst();
            if (delegate.isPresent()) {
                try {
                    // private transient ExtendedLogger logger;
                    Field field = delegate.get();
                    field.setAccessible(true);
                    logger = field.get(logger);
                    clazz = logger.getClass();
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else {
                break;
            }
        }

        List<Method> foundSetLevels = new ArrayList<>();

        // This should work even for relocated classes.
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals("setLevel")) {
                foundSetLevels.add(method);

                if (method.getParameterCount() == 1) {
                    Parameter lvlParameter = method.getParameters()[0];
                    Class<?> type = lvlParameter.getType();

                    // Might be enum values or just
                    Optional<Field> translatedLogLevel = Arrays
                            .stream(type.getDeclaredFields())
                            .filter(x -> XAccessFlag.STATIC.isSet(x.getModifiers()))
                            .filter(x -> tryNames(x.getName(), level))
                            .findFirst();

                    if (translatedLogLevel.isPresent()) {
                        try {
                            method.invoke(logger, translatedLogLevel.get().get(null));
                        } catch (Throwable ex) {
                            throw new IllegalStateException("Cannot set log level of " + loggerString(logger) + " to " + level + " for method " + method, ex);
                        }
                    } else {
                        throw new UnsupportedOperationException("Cannot find any logging levels for " + loggerString(logger) + " for " + level + " for method " + method);
                    }
                }
            }
        }

        throw new UnsupportedOperationException("Cannot find any logging methods for " + loggerString(logger) + " for " + level +
                (foundSetLevels.isEmpty() ? "" : " possible matches: " + foundSetLevels));
    }

    private static String loggerString(Object logger) {
        return logger.getClass().getName() + '(' + logger + ')';
    }

    @SuppressWarnings("RedundantIfStatement")
    private static boolean tryNames(String name, Level level) {
        if (name.equals(level.getName())) return true;
        if (level == Level.FINE || level == Level.FINER || level == Level.FINEST) {
            if (name.equals("DEBUG") || name.equals("TRACE")) return true;
        }
        return false;
    }
}
