package org.kingdoms.services.maps.dynmap;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.*;
import org.kingdoms.main.KLogger;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.server.location.Vector2;
import org.kingdoms.server.location.Vector3Location;
import org.kingdoms.services.managers.SoftService;
import org.kingdoms.services.maps.abstraction.MapAPI;
import org.kingdoms.services.maps.abstraction.markers.IconMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;
import org.kingdoms.services.maps.abstraction.markers.MarkerZoom;
import org.kingdoms.services.maps.abstraction.markersets.MarkerType;
import org.kingdoms.services.maps.abstraction.outliner.Polygon;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Logger;

public final class ServiceDynmap implements MapAPI {
    protected static final String LEAFLET_POPUP_PANES = ".leaflet-popup-content-wrapper";
    protected DynmapCommonAPI api;
    private static final Map<MarkerType, WeakReference<MarkerSet>> MARKERS = new IdentityHashMap<>();
    private final Logger logger;


    public ServiceDynmap(Logger logger) {
        this.logger = logger;

        logger.info("Initializing Dynmap support...");
        DynmapCommonAPIListener.register(new DynmapCommonAPIListener() {
            @Override
            public void apiEnabled(DynmapCommonAPI dynmapCommonAPI) {
                logger.info("Dynmap API is now enabled.");

                api = dynmapCommonAPI;
                for (MarkerType markerType : MarkerType.getMarkerTypes()) {
                    getMarkerSet(markerType); // For error checking purposes.
                }
            }

            @Override
            public void apiDisabled(DynmapCommonAPI api) {
                super.apiDisabled(api);
                logger.severe("Dynmap API was disabled! Map Viewers plugin may not function correctly anymore!");
            }
        });
    }

    MarkerSet getMarkerSet(MarkerType markerType) {
        // When MARKER is null it means the server just started.
        WeakReference<MarkerSet> marker = MARKERS.get(markerType);
        if (marker != null && marker.get() != null) return marker.get();

        if (marker != null) {
            logger.info("Previous Dynmap " + markerType + " marker is no longer available. Creating a new one...");
            MARKERS.remove(markerType);
        }
        MarkerAPI markerAPI;
        try {
            markerAPI = api.getMarkerAPI();
        } catch (Exception ex) {
            logger.severe("Dynmap didn't start correctly, cannot initialize map support. Please check your startup logs to see if Dynmap started correctly without any errors.");
            SoftService.DYNMAP.hook(false);
            ex.printStackTrace();
            return null;
        }

        String id = markerType.getId();
        MarkerSet set = markerAPI.getMarkerSet(id);
        if (set == null) {
            // id is limited to "alphanumerics, periods, underscores"
            set = markerAPI.createMarkerSet(markerType.getId(), markerType.getMarkerSettings().label, null, markerType.getMarkerSettings().persistent);
            Objects.requireNonNull(set, () -> "Dynmap didn't properly create a MarkerSet for " + markerType);
        }

        // set.setLabelShow(true); ???
        set.setLayerPriority(markerType.getMarkerSettings().priority);
        set.setHideByDefault(markerType.getMarkerSettings().hideByDefault);
        MARKERS.put(markerType, new WeakReference<>(set));
        return set;
    }

    @Override
    public Optional<LandMarker> getLandMarker(MarkerType markerType, String id, World world) {
        AreaMarker marker = getMarkerSet(markerType).findAreaMarker(id);
        if (marker == null) return Optional.empty();
        return Optional.of(new LandMarkerDynmap(marker));
    }

    @Override
    public MarkerZoom translateZoom(int min, int max) {
        return translateZoom0(min, max);
    }

    protected static MarkerZoom translateZoom0(int min, int max) {
        // -1 for no limit
        if (min < 0) min = -1;
        if (max < 0) max = -1;

        int dynMin = 0;
        int dynMax = 6;

        // Dynmap has a total of 7 zoom levels:
        // The first is 0 which is the most zoomed out level
        // and 7 which is the most zoomed in level.
        return MarkerZoom.of(
                min == -1 ? -1 : MarkerZoom.valueFromPercent(dynMin, dynMax, 100 - min),
                max == -1 ? -1 : MarkerZoom.valueFromPercent(dynMin, dynMax, max)
        );
    }

    @Override
    public List<LandMarker> createLandMarkers(MarkerType markerType, String id, String label, World world, List<Polygon> polygons, LandMarkerSettings settings) {
        MarkerSet markerSet = getMarkerSet(markerType);
        // Dynmap has no concept of multi-polygon markers, only single polygon markers.
        // To simulate a multi-polygon marker, just create multiple polygon markers.

        // Dynmap also doesn't allow overriding markers
        // so have to delete all of them before creating new ones.
        // Delete all old markers
        {
            int i = 0;
            while (true) {
                AreaMarker marker = markerSet.findAreaMarker(id + '_' + i);
                if (marker != null) marker.deleteMarker();
                else break;
                i++;
            }
        }

        List<LandMarker> markers = new ArrayList<>(polygons.size());

        // Dynmap currently doesn't support negative space
        // https://github.com/webbukkit/dynmap/issues/4076
        // https://github.com/webbukkit/dynmap/issues/3065
        for (int i = 0; i < polygons.size(); i++) {
            Polygon poly = polygons.get(i);
            Collection<Vector2> points = poly.getPoints();
            double[] x = new double[points.size()];
            double[] z = new double[points.size()];

            int j = 0;
            for (Vector2 point : points) {
                x[j] = point.getX();
                z[j] = point.getZ();
                j++;
            }

            String key = id + '_' + i;

            // https://github.com/webbukkit/dynmap/blob/003cad5dc280b68eb675dc7683a87b0ee7b48b58/DynmapCore/src/main/java/org/dynmap/markers/impl/MarkerSetImpl.java#L593-L600
            // Returns null when duplicate ID is encountered.
            AreaMarker areaMarker = markerSet.findAreaMarker(key);
            if (areaMarker != null) areaMarker.deleteMarker();

            areaMarker = markerSet.createAreaMarker(key, label, false, world.getName(), x, z, markerType.getMarkerSettings().persistent);
            Objects.requireNonNull(areaMarker, () -> "Dynmap didn't create area marker for " + markerType + " with id " + key);
            Objects.requireNonNull(areaMarker.getMarkerSet(), () -> "Dynmap didn't assign any marker set for " + markerType + " with id " + key + ", which was expected to be: " + markerSet); // Read LandMarkerDynmap.setSettings() for more info
            markers.add(new LandMarkerDynmap(areaMarker));
        }

        return markers;
    }

    @Override
    public boolean isEnabledInWorld(org.kingdoms.server.location.World world) {
        // For Dynmap, it's enabled in all worlds.
        return true;
    }

    @Override
    public void removeIconMarker(MarkerType markerType, String id, Vector3Location location) {
        Marker icon = getMarkerSet(markerType).findMarker(id);
        if (icon != null) icon.deleteMarker();
    }

    @Override
    public void sendMessage(Player player, String message) {
        api.postPlayerMessageToWeb(player.getUniqueId().toString(), player.getDisplayName(), message);
    }

    @Override
    public String createImage(BufferedImage image, String fileName) {
        // https://github.com/webbukkit/dynmap/tree/v3.0/DynmapCore/src/main/resources/extracted/web/images
        File dataFolder;
        try {
            Class<?> clazz = Class.forName("org.dynmap.DynmapCore");
            Method getDataFolder = clazz.getDeclaredMethod("getDataFolder");
            dataFolder = (File) getDataFolder.invoke(api);
        } catch (ClassNotFoundException | NoSuchMethodException |
                 IllegalAccessException | InvocationTargetException e) {
            Path imgsFolder = Kingdoms.getFolder().getParent().resolve("dynmap").resolve("images");
            if (!Files.exists(imgsFolder)) {
                throw new IllegalStateException("Cannot find Dynmap's images folder: " + imgsFolder, e);
            } else {
                dataFolder = imgsFolder.toFile();
            }
        }

        Path dataFolderPath = dataFolder.toPath();
        return MapAPI.createImage(image, fileName, dataFolderPath, dataFolderPath.resolve("images"), null);
    }

    @Override
    public String getPopupContainerSelector() {
        return LEAFLET_POPUP_PANES;
    }

    @Override
    public IconMarker updateOrAddIcon(MarkerType markerType, String id, LandMarkerSettings settings, Vector3Location location, Object icon) {
        MarkerSet markerSet = getMarkerSet(markerType);
        Marker marker = markerSet.findMarker(id);
        String world = location.getWorld().getName();

        if (marker == null) {
            marker = markerSet.createMarker(id, settings.getClickDescription(), true,
                    world, location.getX(), location.getY(), location.getZ(), (MarkerIcon) icon, markerType.getMarkerSettings().persistent);
            Objects.requireNonNull(marker, () -> "Dynmap's createMarker() returned null for " + markerType + " -> " + id + " -> " + markerSet.findMarker(id));
        } else {
            marker.setLocation(world, location.getX(), location.getY(), location.getZ());
            marker.setMarkerIcon((MarkerIcon) icon);
            marker.setLabel(settings.getClickDescription());
        }

        marker.setMinZoom(settings.getZoomMin());
        marker.setMaxZoom(settings.getZoomMax());
        return new IconMarkerDynmap(marker);
    }

    @Override
    public void removeEverything() {
        for (WeakReference<MarkerSet> marker : MARKERS.values()) {
            MarkerSet set = marker.get();
            if (set == null) return;
            set.deleteMarkerSet();
        }
        MARKERS.clear();
    }

    @Override
    public Object updateOrRegisterIcon(String id, Path path) {
        id = id.replace('/', '-');
        MarkerIcon icon = api.getMarkerAPI().getMarkerIcon(id);
        if (icon != null) return icon;

        InputStream is = null;
        try {
            is = new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // For some reason, Dynmap doesn't print the stacktrace if it fails to register an icon.
        // It just logs a message: Error copying marker - {$path}

        // Note: The second parameter is the "label" and I have no idea how it's used.
        // Note: Dynmap uses the "id" as the file name.

        // https://github.com/webbukkit/dynmap/blob/003cad5dc280b68eb675dc7683a87b0ee7b48b58/DynmapCore/src/main/java/org/dynmap/markers/impl/MarkerAPIImpl.java#L755-L756
        MarkerIcon created = api.getMarkerAPI().createMarkerIcon(id, "Kingdoms", is);
        return Objects.requireNonNull(created, "Failed to create Dynmap icon with id ' " + id + "' and path: " + path);
    }
}