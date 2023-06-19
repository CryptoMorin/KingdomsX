package org.kingdoms.utils.internal.jdk;

import javax.tools.JavaCompiler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public final class Java9 {
    public static Method getMethodOfCaller(int depth) {
        StackWalker.StackFrame caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(x -> x.skip(depth).findFirst().orElseThrow(
                        () -> new IllegalArgumentException("Method caller depth too deep: " + depth)));

        try {
            return caller.getDeclaringClass().getMethod(caller.getMethodName(), caller.getMethodType().parameterArray());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
