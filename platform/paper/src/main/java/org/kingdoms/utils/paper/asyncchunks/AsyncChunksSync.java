package org.kingdoms.utils.paper.asyncchunks;

import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

final class AsyncChunksSync extends AsyncChunks {
    @Override
    public CompletableFuture<Chunk> getChunkAtAsync(World world, int x, int z, boolean gen, boolean isUrgent) {
        return CompletableFuture.completedFuture(world.getChunkAt(x, z));
    }
}