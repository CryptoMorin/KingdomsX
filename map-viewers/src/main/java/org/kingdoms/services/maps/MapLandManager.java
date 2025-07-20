package org.kingdoms.services.maps;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kingdoms.constants.group.Group;
import org.kingdoms.constants.land.location.SimpleChunkLocation;
import org.kingdoms.main.KLogger;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.managers.invasions.Invasion;
import org.kingdoms.platform.bukkit.location.BukkitWorld;
import org.kingdoms.server.location.Vector2;
import org.kingdoms.server.location.World;
import org.kingdoms.services.maps.abstraction.MapAPI;
import org.kingdoms.services.maps.abstraction.markers.LandMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;
import org.kingdoms.services.maps.abstraction.markersets.MarkerType;
import org.kingdoms.services.maps.abstraction.outliner.*;
import org.kingdoms.utils.internal.uuid.FastUUID;

import java.util.*;
import java.util.stream.Collectors;

public final class MapLandManager {
    private static final String INVASION_ID_PREFIX = ".";
    private static final int CHUNK_BLOCK_SIZE = 16;

    // Map<Group, Map<World, List<LandMarker>>>
    private final Map<UUID, Map<UUID, List<LandMarker>>> areas = new HashMap<>();
    private final Map<Invasion, List<LandMarker>> invasionAreas = new IdentityHashMap<>();

    private final MarkerType markerType;
    private final MapAPI api;

    public MapLandManager(MarkerType markerType, MapAPI api) {
        this.markerType = markerType;
        this.api = api;
    }

    public void createAreasFromWorlds(
            Group group, Map<String, ? extends Collection<SimpleChunkLocation>> chunks,
            LandMarkerSettings settings, String idSuffix) {
        for (Map.Entry<String, ? extends Collection<SimpleChunkLocation>> chunk : chunks.entrySet()) {
            World world = Kingdoms.getServerX().getWorldRegistry().getWorld(chunk.getKey());
            if (world == null) {
                MapViewerAddon.get().getLogger().severe("Cannot find world '" + chunk.getKey() + "' while updating map chunks for " + group.getName());
                continue;
            }
            if (!api.isEnabledInWorld(world) || !ServiceMap.isEnabledInWorld(world)) {
                KLogger.debug(ServiceMap.DEBUG_NS, () -> "The world " + world.getName() + " was disabled for map viewers, skipping... "
                        + api.isEnabledInWorld(world) + " - " + ServiceMap.isEnabledInWorld(world));
                continue;
            }
            createAreas(world, group, null, chunk.getValue(), settings, idSuffix);
        }
    }

    public void removeInvadedAreas(Invasion invasion) {
        List<LandMarker> markers = invasionAreas.remove(invasion);
        if (markers == null) return;
        for (LandMarker marker : markers) marker.delete();
    }

    public void removeAreasOf(Group group) {
        Map<UUID, List<LandMarker>> markers = areas.remove(group.getId());
        if (markers == null) return;
        for (List<LandMarker> worldMarkers : markers.values()) {
            for (LandMarker marker : worldMarkers) {
                marker.delete();
            }
        }
    }

    public void createAreas(org.kingdoms.server.location.World world, Group group, @Nullable Invasion invasion,
                            Collection<SimpleChunkLocation> chunks, LandMarkerSettings settings, String idSuffix) {
        final Collection<WorldlessChunk> normalizedChunks = chunks.stream()
                .map(x -> new WorldlessChunk(x.getX(), x.getZ()))
                .collect(Collectors.toList());

        // Sort the chunk into clusters
        List<Polygon> parts = getPolygons(normalizedChunks);

        if (!parts.isEmpty()) {
            String markerId = (invasion == null ? "" : INVASION_ID_PREFIX)
                    + this.markerType.getNamespace().asNormalizedString()
                    + world.getName() + '_' + FastUUID.toString(group.getId()) + '_' + parts.size()
                    + (idSuffix == null || idSuffix.isEmpty() ? "" : '_' + idSuffix);

            List<LandMarker> markers = api.createLandMarkers(markerType, markerId, group.getName(), ((BukkitWorld) world).getWorld(), parts, settings);
            for (LandMarker marker : markers) marker.setSettings(settings);
            KLogger.debug(ServiceMap.DEBUG_NS, () -> "Created marker with ID " + markerId + " using API " + markerType + "::" + api + " for a total of " + chunks.size() + " chunks.");

            if (invasion != null) invasionAreas.put(invasion, markers);
            else {
                areas.compute(group.getId(), (k, v) -> {
                    if (v == null) v = new HashMap<>(2);
                    v.compute(world.getId(), (k2, v2) -> {
                        // Create a new list, so we don't rely on the one returned by the implementation.
                        if (v2 == null) return new ArrayList<>(markers);
                        else {
                            v2.addAll(markers);
                            return v2;
                        }
                    });
                    return v;
                });
            }
        } else {
            KLogger.debug(ServiceMap.DEBUG_NS, () -> "No markers were created for " + group + " in world " + world.getName() + " for chunks "
                    + chunks.size() + " using API " + api + " - " + markerType + '.');
        }
    }

    private static @NotNull List<Polygon> getPolygons(Collection<WorldlessChunk> blocks) {
        List<ConnectedChunkCluster> clusters = ConnectedChunkCluster.findClusters(blocks);
        List<Polygon> parts = new ArrayList<>(clusters.size());

        for (ConnectedChunkCluster cluster : clusters) {
            // Check if the cluster has negative space
            Collection<WorldlessChunk> negativeSpace = NegativeSpaceOutliner.findNegativeSpace(cluster);
            List<List<Vector2>> negSpacePolys = Collections.emptyList();

            // If the cluster does have negative space, get the outlines of the negative space polygons
            if (!negativeSpace.isEmpty()) {
                negSpacePolys = ConnectedChunkCluster.findClusters(negativeSpace).stream()
                        .map(subCluster -> PolygonOutliner.findPointsOf(subCluster, CHUNK_BLOCK_SIZE))
                        .collect(Collectors.toList());
            }

            // Form the main polygon
            List<Vector2> mainPolygonPoints = PolygonOutliner.findPointsOf(cluster, CHUNK_BLOCK_SIZE);
            Polygon part = new Polygon(mainPolygonPoints, negSpacePolys);
            parts.add(part);
        }

        return parts;
    }

    public void clearAreas() {
        areas.clear();
        invasionAreas.clear();
    }

    public void clearAreasAndDeleteMarkers() {
        areas.values().forEach(x -> x.values().forEach(y -> y.forEach(LandMarker::delete)));
        invasionAreas.values().forEach(x -> x.forEach(LandMarker::delete));
        clearAreas();
    }
}
