package org.kingdoms.utils.internal.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    public static <T> CompletableFuture<T> orTimeout(CompletableFuture<T> future, long timeout, TimeUnit unit) {
        return future.orTimeout(timeout, unit);
    }
}
