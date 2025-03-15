package org.kingdoms.enginehub.worldguard.handlers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kingdoms.constants.land.location.SimpleChunkLocation;
import org.kingdoms.enginehub.worldguard.ServiceWorldGuard;
import org.kingdoms.events.lands.LandChangeEvent;
import org.kingdoms.managers.land.indicator.LandIndicatorPreparation;
import org.kingdoms.managers.land.indicator.LandVisualizer;

public class WorldGuardLandVisualizerPreparation implements LandIndicatorPreparation {
    private final ServiceWorldGuard worldGuard;

    public WorldGuardLandVisualizerPreparation(ServiceWorldGuard worldGuard) {this.worldGuard = worldGuard;}

    @Nullable
    @Override
    public LandVisualizer prepare(@NotNull LandChangeEvent event, @NotNull LandVisualizer visualizer) {
        SimpleChunkLocation toChunk = event.getToChunk();
        if (worldGuard.isChunkInRegion(toChunk.getBukkitWorld(), toChunk.getX(), toChunk.getZ(), 0)) return null;
        return visualizer;
    }
}
