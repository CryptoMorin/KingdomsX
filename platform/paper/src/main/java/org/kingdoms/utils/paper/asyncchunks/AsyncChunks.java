package org.kingdoms.utils.paper.asyncchunks;

import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class AsyncChunks {
    public static AsyncChunks generateInstance(Function<Integer, Boolean> versionChecker) {
        if (versionChecker.apply(15)) {
            return new AsyncChunksPaper_15();
        } else if (versionChecker.apply(13)) {
            return new AsyncChunksPaper_13();
        } else if (versionChecker.apply(9) && !versionChecker.apply(13)) {
            return new AsyncChunksPaper_9_12();
        } else {
            // noinspection StaticInitializerReferencesSubClass
            return new AsyncChunksSync();
        }
    }

    public final CompletableFuture<Chunk> getChunkAtAsync(World world, int x, int z) {
        return getChunkAtAsync(world, x, z, true, false);
    }

    public final CompletableFuture<Chunk> getChunkAtAsync(World world, int x, int z, boolean generate) {
        return getChunkAtAsync(world, x, z, generate, false);
    }

    public abstract CompletableFuture<Chunk> getChunkAtAsync(World world, int x, int z, boolean generate, boolean isUrgent);
}