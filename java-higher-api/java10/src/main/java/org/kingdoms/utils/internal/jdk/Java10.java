package org.kingdoms.utils.internal.jdk;

import java.lang.reflect.Method;

public final class Java10 {
    /**
     * Stack walker was introduced in Java 9, but certain methods that are
     * used here were added later in Java 10.
     */
    public static Method getCallerMethod(int depth) {
        StackWalker.StackFrame caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(x -> x.skip(depth).findFirst().orElseThrow(
                        () -> new IllegalArgumentException("Method caller depth too deep: " + depth)));

        try {
            Class<?>[] parameterTypes = caller.getMethodType().parameterArray();
            return caller.getDeclaringClass().getMethod(caller.getMethodName(), parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
