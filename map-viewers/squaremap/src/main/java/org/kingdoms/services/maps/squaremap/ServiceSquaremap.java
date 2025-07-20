package org.kingdoms.services.maps.squaremap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.kingdoms.main.KLogger;
import org.kingdoms.platform.bukkit.location.BukkitWorld;
import org.kingdoms.server.location.Vector2;
import org.kingdoms.server.location.Vector3Location;
import org.kingdoms.services.maps.abstraction.MapAPI;
import org.kingdoms.services.maps.abstraction.markers.IconMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;
import org.kingdoms.services.maps.abstraction.markers.MarkerZoom;
import org.kingdoms.services.maps.abstraction.markersets.MarkerType;
import xyz.jpenilla.squaremap.api.*;
import xyz.jpenilla.squaremap.api.marker.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * https://github.com/jpenilla/squaremap/wiki/API-Basics
 */
public final class ServiceSquaremap implements MapAPI {
    protected static final String POPUP_CSS_SELECTOR = ".leaflet-popup-content-wrapper";
    private static final int ICON_SIZE = 32;
    private final Map<UUID, Map<MarkerType, SimpleLayerProvider>> layers = new HashMap<>(3);
    private final Logger logger;

    public ServiceSquaremap(Logger logger) {
        this.logger = logger;
        logger.info("Initializing squaremap support...");
    }

    private static Squaremap getAPI() {
        try {
            return SquaremapProvider.get();
        } catch (IllegalStateException ex) {
            throw new RuntimeException("Squaremap API is not loaded yet", ex);
        }
    }

    private Key id(String id) {
        return Key.of(normalizeId(id));
    }

    @Override
    public void removeIconMarker(MarkerType markerType, String id, Vector3Location location) {
        SimpleLayerProvider layer = getLayer(markerType, org.kingdoms.platform.bukkit.adapters.BukkitAdapter.adapt(location.getWorld()));
        if (layer == null) return;
        layer.removeMarker(Key.of(id));
    }

    @Override
    public String getPopupContainerSelector() {
        return POPUP_CSS_SELECTOR;
    }

    @Override
    public Optional<LandMarker> getLandMarker(MarkerType markerType, String id, World world) {
        SimpleLayerProvider layer = getLayer(markerType, world);
        Key key = id(id);
        Polygon marker = (Polygon) layer.registeredMarkers().get(key);

        return marker == null ? Optional.empty() : Optional.of(new LandMarkerSquaremap(marker, key, layer));
    }

    private SimpleLayerProvider getLayer(MarkerType markerType, World world) {
        if (markerType.getMarkerSettings().isWorldDisabled.apply(org.kingdoms.platform.bukkit.adapters.BukkitAdapter.adapt(world))) {
            throw new IllegalArgumentException("Attempting to get layer in disabled world: " + world.getName());
        }

        Map<MarkerType, SimpleLayerProvider> worldLayers = this.layers.computeIfAbsent(world.getUID(), k -> new IdentityHashMap<>());
        SimpleLayerProvider layer = worldLayers.get(markerType);
        if (layer == null) {
            Squaremap api = getAPI();
            MapWorld map = api.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world))
                    .orElseThrow(() -> new IllegalArgumentException("No squaremap world for: " + world.getName()));
            Registry<LayerProvider> layerRegistry = map.layerRegistry();
            Key key = Key.of(markerType.getId());

            if (layerRegistry.hasEntry(key)) {
                // Squaremap doesn't allow us to get() without doing a contains() check first... FFS
                // https://github.com/jpenilla/squaremap/blob/3746f4bf5700248c5f44608f155d90a0688fdc12/api/src/main/java/xyz/jpenilla/squaremap/api/Registry.java#L37-L44
                layer = (SimpleLayerProvider) layerRegistry.get(key);
            } else {
                layer = SimpleLayerProvider.builder(markerType.getMarkerSettings().label)
                        .showControls(markerType.getMarkerSettings().showControls)
                        .defaultHidden(markerType.getMarkerSettings().hideByDefault)
                        .layerPriority(markerType.getMarkerSettings().priority)
                        .zIndex(markerType.getMarkerSettings().zIndex)
                        .build();

                layerRegistry.register(key, layer);
            }

            worldLayers.put(markerType, layer);
        }

        return layer;
    }

    @Override
    public MarkerZoom translateZoom(int min, int max) {
        return MarkerZoom.of(min, max);
    }

    @Override
    public List<LandMarker> createLandMarkers(MarkerType markerType, String id, String label, World world, List<org.kingdoms.services.maps.abstraction.outliner.Polygon> polygons, LandMarkerSettings settings) {
        List<MultiPolygon.MultiPolygonPart> parts = new ArrayList<>(polygons.size());
        for (org.kingdoms.services.maps.abstraction.outliner.Polygon polygon : polygons) {
            List<Point> polyPoints = toPoints(polygon.getPoints());
            List<List<Point>> negSpace = polygon.getNegativeSpace().stream()
                    .map(ServiceSquaremap::toPoints)
                    .collect(Collectors.toList());
            parts.add(MultiPolygon.part(polyPoints, negSpace));
        }

        MultiPolygon marker = MultiPolygon.multiPolygon(parts);
        SimpleLayerProvider layer = getLayer(markerType, world);
        Key key = id(id);

        layer.addMarker(key, marker);
        return Collections.singletonList(new LandMarkerSquaremap(marker, key, layer));
    }

    private static List<Point> toPoints(Collection<Vector2> point2Ds) {
        List<Point> points = new ArrayList<>(point2Ds.size());
        for (Vector2 point2D : point2Ds) {
            points.add(Point.of(point2D.getX(), point2D.getZ()));
        }
        return points;
    }

    @Override
    public boolean isEnabledInWorld(org.kingdoms.server.location.World world) {
        return SquaremapProvider.get().getWorldIfEnabled(BukkitAdapter.worldIdentifier(((BukkitWorld) world).getWorld())).isPresent();
    }

    @Override
    public String normalizeId(String id) {
        // [a-zA-Z0-9._-]
        // , for locations
        // / for icon locations
        return id.replace(',', '.').replace('/', '.');
    }

    @Override
    public IconMarker updateOrAddIcon(MarkerType markerType, String id, LandMarkerSettings settings, Vector3Location location, Object icon) {
        SimpleLayerProvider layer = getLayer(markerType, org.kingdoms.platform.bukkit.adapters.BukkitAdapter.adapt(location.getWorld()));
        Key key = id(id);
        Icon marker = (Icon) layer.registeredMarkers().get(key);
        if (marker == null) {
            marker = Marker.icon(Point.of(location.getX(), location.getZ()), (Key) icon, ICON_SIZE);
            layer.addMarker(key, marker);
        } else {
            marker.image((Key) icon);
            marker.point(Point.of(location.getX(), location.getZ()));
        }

        marker.markerOptions(MarkerOptions.builder().clickTooltip(settings.getClickDescription()).build());
        return new IconMarkerSquaremap(marker, key, layer);
    }

    @Override
    public String createImage(BufferedImage image, String fileName) {
        // https://github.com/jpenilla/squaremap/blob/24f14ef6ef2cbf15fbd7c899d0d73f91440b021e/common/src/main/java/xyz/jpenilla/squaremap/common/IconRegistry.java#L24
        // https://github.com/jpenilla/squaremap/blob/24f14ef6ef2cbf15fbd7c899d0d73f91440b021e/common/src/main/java/xyz/jpenilla/squaremap/common/data/LevelBiomeColorData.java#L28
        // directoryProvider.webDirectory().resolve("images/icon/registered/");
        Path root = SquaremapProvider.get().webDir();
        return MapAPI.createImage(image, fileName, root, root.resolve("images"), null);
    }

    @Override
    public void removeEverything() {
        Squaremap api = getAPI();
        for (Map.Entry<UUID, Map<MarkerType, SimpleLayerProvider>> world : layers.entrySet()) {
            World bukkitWorld = Bukkit.getWorld(world.getKey());
            if (bukkitWorld == null) continue;

            Optional<MapWorld> map = api.getWorldIfEnabled(BukkitAdapter.worldIdentifier(bukkitWorld));
            if (!map.isPresent()) continue;

            Registry<LayerProvider> registry = map.get().layerRegistry();
            for (MarkerType layerId : world.getValue().keySet()) {
                Key key = Key.of(layerId.getId());
                if (registry.hasEntry(key)) registry.unregister(key);
            }
        }
        this.layers.clear();
    }

    @Override
    public Object updateOrRegisterIcon(String id, Path path) {
        Registry<BufferedImage> registry = SquaremapProvider.get().iconRegistry();
        Key key = id(id);

        // For some reason, Squaremap hasEntry() returns false while the image is already registered,
        // causing the code below to throw an error...
        // IllegalArgumentException: Image already registered for key 'powercell.1'
        // This seems to specifically happen to powercell icons?
        synchronized (registry) {
            if (registry.hasEntry(key)) return key;

            try {
                InputStream is = new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ));
                registry.register(key, ImageIO.read(is));
                return key;
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("Image already registered")) {
                    KLogger.warn("Squaremap incorrectly warned about image being registerd twice for: '"
                            + id + "' -> '" + key + "' and path '" + path + "' you can ignore this warning. " + e.getMessage());
                    return registry.get(key);
                } else {
                    throw new IllegalArgumentException("Failed to register icon for squaremap with id '"
                            + id + "' -> '" + key + "' and path '" + path + '\'', e);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to register icon for squaremap with id '"
                        + id + "' -> '" + key + "' and path '" + path + '\'', e);
            }
        }
    }
}