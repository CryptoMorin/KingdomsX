package org.kingdoms.utils.internal.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class Java9 {
    public static Class<?> getCallerClass() {
        return StackWalker
                .getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .getCallerClass();
    }

    public static byte[] readAllBytes(InputStream input) throws IOException {
        return input.readAllBytes();
    }

    public static long transferTo(InputStream input, OutputStream output) throws IOException {
        return input.transferTo(output);
    }
}
