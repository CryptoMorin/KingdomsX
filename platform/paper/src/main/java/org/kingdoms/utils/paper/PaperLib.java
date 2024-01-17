package org.kingdoms.utils.paper;

import org.kingdoms.utils.paper.asyncchunks.AsyncChunks;

import java.util.function.Function;

public final class PaperLib {
    private static AsyncChunks asyncChunks;

    public static void init(Function<Integer, Boolean> versionChecker) {
        asyncChunks = AsyncChunks.generateInstance(versionChecker);
    }

    public static AsyncChunks getAsyncChunks() {
        return asyncChunks;
    }
}
