package org.kingdoms.services.maps.abstraction.outliner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A cluster represents a group of {@link WorldlessChunk}s which are all connected.
 * Only the {@link #findClusters(Collection)} is intended to be used as an API.
 * <p>
 * This is similar to {@link org.kingdoms.utils.chunks.ChunkConnections#getConnectedClusters(int, Set)},
 * however it's specialized for this addon specifically.
 */
public final class ConnectedChunkCluster {
    private final Set<WorldlessChunk> chunks = new HashSet<>();

    private ConnectedChunkCluster() {}

    public int size() {
        return chunks.size();
    }

    public boolean has(WorldlessChunk hash) {
        return chunks.contains(hash);
    }

    private void add(WorldlessChunk chunk) {
        chunks.add(chunk);
    }

    /**
     * Gets a random chunk in the cluster or null if none.
     */
    @Nullable
    public WorldlessChunk findAny() {
        return chunks.stream().findAny().orElse(null);
    }

    /**
     * An immutable collection of all the unique chunks in the cluster.
     */
    @NotNull
    public Collection<WorldlessChunk> getChunks() {
        return Collections.unmodifiableCollection(chunks);
    }

    /**
     * Create clusters from groups of connected {@link WorldlessChunk}s.
     * <br>
     * Two chunks are considered connected only if one of their borders/edges
     * completely touch each other. So diagonal chunks are not considered connected.
     *
     * @param chunks Collection of chunks to find its clusters.
     * @return a list of connected chunk clusters.
     */
    @NotNull
    public static List<ConnectedChunkCluster> findClusters(@NotNull Collection<WorldlessChunk> chunks) {
        Objects.requireNonNull(chunks, "Chunk clusters are null");
        if (chunks.isEmpty()) return Collections.emptyList();

        Set<WorldlessChunk> remainingChunks = new HashSet<>(chunks);
        List<ConnectedChunkCluster> clusters = new ArrayList<>();
        Deque<WorldlessChunk> pendingVisits = new ArrayDeque<>(); // Can be reused for all clusters. Don't use Stack, they're synchronized.

        while (!remainingChunks.isEmpty()) {
            ConnectedChunkCluster cluster = new ConnectedChunkCluster();

            // Push the first claim for the next cluster.
            pendingVisits.push(remainingChunks.iterator().next());
            do {
                WorldlessChunk next = pendingVisits.pop();

                // Chunk was already visited
                if (!remainingChunks.remove(next)) continue;
                cluster.add(next);

                // Check above, below, right and left of the chunk
                for (ChunkDirection direction : ChunkDirection.DIRECTIONS) {
                    WorldlessChunk relative = next.offset(direction);
                    if (remainingChunks.contains(relative))
                        pendingVisits.push(relative);
                }
            } while (!pendingVisits.isEmpty());

            // The only time this might be empty is if we already visited
            // this chunk as a result of previous chained visits.
            if (!cluster.chunks.isEmpty())
                clusters.add(cluster);
        }

        return clusters;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '(' + chunks + ')';
    }
}
