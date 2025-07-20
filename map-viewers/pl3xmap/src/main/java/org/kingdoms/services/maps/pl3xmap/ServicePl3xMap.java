package org.kingdoms.services.maps.pl3xmap;

import com.cryptomorin.xseries.reflection.XReflection;
import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.image.IconImage;
import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.markers.layer.Layer;
import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.markers.marker.Icon;
import net.pl3x.map.core.markers.marker.MultiPolygon;
import net.pl3x.map.core.markers.marker.Polyline;
import net.pl3x.map.core.markers.option.Options;
import net.pl3x.map.core.markers.option.Tooltip;
import net.pl3x.map.core.registry.IconRegistry;
import net.pl3x.map.core.registry.Registry;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.kingdoms.platform.bukkit.adapters.BukkitAdapter;
import org.kingdoms.server.location.Vector2;
import org.kingdoms.server.location.Vector3Location;
import org.kingdoms.services.maps.abstraction.MapAPI;
import org.kingdoms.services.maps.abstraction.markers.IconMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;
import org.kingdoms.services.maps.abstraction.markers.MarkerZoom;
import org.kingdoms.services.maps.abstraction.markersets.MarkerSettings;
import org.kingdoms.services.maps.abstraction.markersets.MarkerType;
import org.kingdoms.services.maps.abstraction.outliner.Polygon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Logger;

public final class ServicePl3xMap implements MapAPI {
    protected static final String POPUP_CSS_SELECTOR = ".leaflet-popup-content-wrapper";
    private static final int ICON_SIZE = 32;
    private static final Map<String, Map<MarkerType, SimpleLayer>> LAYERS = new HashMap<>(3);
    private final Logger logger;

    public ServicePl3xMap(Logger logger) {
        this.logger = logger;
        logger.info("Initializing Pl3xMap support...");

        Pl3xMap api = Pl3xMap.api();
        for (net.pl3x.map.core.world.World map : api.getWorldRegistry().values()) {
            World world = Bukkit.getWorld(map.getName());
            Map<MarkerType, SimpleLayer> markers = new IdentityHashMap<>();
            Registry<Layer> layerRegistry = map.getLayerRegistry();

            for (MarkerType markerType : MarkerType.getMarkerTypes()) {
                MarkerSettings markerSettings = markerType.getMarkerSettings();
                if (markerSettings.isWorldDisabled.apply(BukkitAdapter.adapt(world))) continue;

                SimpleLayer layer = (SimpleLayer) layerRegistry.get(markerType.getId());
                if (layer == null) {
                    // https://billygalbreath.github.io/Pl3xMap/net/pl3x/map/markers/layer/Layer.html
                    layer = new SimpleLayer(markerType.getId(), () -> markerSettings.label);
                    // layer.setPane() No clue what this is because the docs are amazingly accurate.
                    layerRegistry.register(markerType.getId(), layer);
                }

                layer.setShowControls(markerSettings.showControls);
                layer.setDefaultHidden(markerSettings.hideByDefault);
                layer.setZIndex(markerSettings.zIndex);
                layer.setPriority(markerSettings.priority);

                markers.put(markerType, layer);
            }

            LAYERS.put(world.getName(), markers);
        }
    }

    @Override
    public Optional<LandMarker> getLandMarker(MarkerType markerType, String key, World world) {
        SimpleLayer layer = getLayer(world.getName(), markerType);
        MultiPolygon marker = (MultiPolygon) layer.registeredMarkers().get(key);

        return marker == null ? Optional.empty() : Optional.of(new LandMarkerPl3xMap(marker, key, layer));
    }

    protected static SimpleLayer getLayer(String world, MarkerType markerType) {
        Map<MarkerType, SimpleLayer> worldContainer = Objects.requireNonNull(LAYERS.get(world),
                () -> "No layer is provided for world " + world + " for marker " + markerType);
        return Objects.requireNonNull(worldContainer.get(markerType),
                () -> "No layer is provided for " + markerType + " in world " + world);
    }

    @Override
    public List<LandMarker> createLandMarkers(MarkerType markerType, String key, String label, World world, List<Polygon> polygons, LandMarkerSettings settings) {
        Collection<net.pl3x.map.core.markers.marker.Polygon> parts = new ArrayList<>(polygons.size());
        for (org.kingdoms.services.maps.abstraction.outliner.Polygon polygon : polygons) {
            List<Point> polyPoints = toPoints(polygon.getPoints());
            parts.add(net.pl3x.map.core.markers.marker.Polygon.of("0", Polyline.of("0", polyPoints)));
        }

        MultiPolygon marker = MultiPolygon.of(key, parts);
        SimpleLayer layer = getLayer(world.getName(), markerType);

        layer.addMarker(marker);
        return Collections.singletonList(new LandMarkerPl3xMap(marker, key, layer));
    }

    @Override
    public String getPopupContainerSelector() {
        return POPUP_CSS_SELECTOR;
    }

    private static final MethodHandle Point$of = XReflection.of(Point.class)
            .method("public static Point of(double x, double z)")
            .unreflect();

    private static final MethodHandle Icon$of = XReflection.of(Icon.class)
            .method("public static Icon of(String key, net.pl3x.map.core.markers.Point point, String image, double size)")
            .unreflect();

    public static Icon of(String key, Point point, String image, double size) {
        try {
            return (Icon) Icon$of.invokeExact(key, point, image, size);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Point of(double x, double z) {
        try {
            return (Point) Point$of.invokeExact(x, z);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Point> toPoints(Collection<Vector2> point2Ds) {
        List<Point> points = new ArrayList<>(point2Ds.size());
        for (Vector2 point2D : point2Ds) {
            points.add(of(point2D.getX(), point2D.getZ()));
        }
        return points;
    }

    @Override
    public String createImage(BufferedImage image, String fileName) {
        // FileUtil.getWebDir().resolve("images/icon/registered/");
        Path imagesFolder = Pl3xMap.api().getIconRegistry().getDir().getParent();
        return MapAPI.createImage(image, fileName, imagesFolder.getParent(), imagesFolder, null);
    }

    @Override
    public boolean isEnabledInWorld(org.kingdoms.server.location.World world) {
        return Pl3xMap.api().getWorldRegistry().has(world.getName());
    }

    @Override
    public void removeIconMarker(MarkerType markerType, String id, Vector3Location location) {
        SimpleLayer layer = getLayer(location.getWorld().getName(), markerType);
        if (layer == null) return;
        layer.removeMarker(id);
    }

    @Override
    public IconMarker updateOrAddIcon(MarkerType markerType, String id, LandMarkerSettings settings, Vector3Location location, Object icon) {
        SimpleLayer layer = getLayer(location.getWorld().getName(), markerType);
        Icon marker = (Icon) layer.registeredMarkers().get(id);
        Point point = of(location.getX(), location.getZ());

        if (marker == null) {
            marker = of(id, point, (String) icon, ICON_SIZE);
            layer.addMarker(marker);
        } else {
            marker.setImage((String) icon);
            marker.setPoint(point);
        }

        marker.setOptions(Options.builder().tooltip(new Tooltip(settings.getClickDescription())).build());
        return new IconMarkerPl3xMap(marker, id, layer);
    }

    @Override
    public void removeEverything() {
        LAYERS.values().forEach(x -> x.values().forEach(SimpleLayer::clearMarkers));
    }

    @Override
    public Object updateOrRegisterIcon(String key, Path path) {
        IconRegistry registry = Pl3xMap.api().getIconRegistry();
        if (registry.has(key)) return key;

        try {
            // https://billygalbreath.github.io/Pl3xMap/net/pl3x/map/image/IconImage.html
            // "image type", thank you, very cool, very helpful!
            InputStream is = new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ));

            String fileName = key.replace('/', '-');
            registry.register(fileName, new IconImage(fileName, ImageIO.read(is), "png"));
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to register icon for squaremap", e);
        }
    }

    @Override
    public MarkerZoom translateZoom(int min, int max) {
        return MarkerZoom.of(min, max);
    }
}