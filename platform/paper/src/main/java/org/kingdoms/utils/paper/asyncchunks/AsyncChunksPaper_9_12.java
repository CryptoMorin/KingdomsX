package org.kingdoms.utils.paper.asyncchunks;

import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

/**
 * Async Chunk Loading for Paper version 1.9 to 1.12
 */
final class AsyncChunksPaper_9_12 extends AsyncChunks {
    private static final boolean SUPPORTS_IS_CHUNK_GENERATED;

    static {
        boolean isChunkGenerated = false;

        try {
            World.class.getMethod("isChunkGenerated", int.class, int.class);
            isChunkGenerated = true;
        } catch (NoSuchMethodException ignored) {
        }

        SUPPORTS_IS_CHUNK_GENERATED = isChunkGenerated;
    }

    @SuppressWarnings("deprecation")
    @Override
    public CompletableFuture<Chunk> getChunkAtAsync(World world, int x, int z, boolean gen, boolean isUrgent) {
        CompletableFuture<Chunk> future = new CompletableFuture<>();
        if (!gen && SUPPORTS_IS_CHUNK_GENERATED && !world.isChunkGenerated(x, z)) {
            future.complete(null);
        } else {
            World.ChunkLoadCallback chunkLoadCallback = future::complete;
            world.getChunkAtAsync(x, z, chunkLoadCallback);
        }
        return future;
    }
}