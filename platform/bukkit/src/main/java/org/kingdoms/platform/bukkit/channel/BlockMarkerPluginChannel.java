package org.kingdoms.platform.bukkit.channel;

import org.jetbrains.annotations.Unmodifiable;
import org.kingdoms.server.location.BlockVector3;

import java.util.*;

public final class BlockMarkerPluginChannel {
    private final Map<BlockVector3, BlockMarker> markers;

    public BlockMarkerPluginChannel(Map<BlockVector3, BlockMarker> affectedBlocks) {
        this.markers = Objects.requireNonNull(affectedBlocks);
        if (affectedBlocks.isEmpty()) throw new IllegalStateException("Affected blocks is empty");
    }

    @Unmodifiable
    public Map<BlockVector3, BlockMarker> getMarkers() {
        return Collections.unmodifiableMap(markers);
    }
}
