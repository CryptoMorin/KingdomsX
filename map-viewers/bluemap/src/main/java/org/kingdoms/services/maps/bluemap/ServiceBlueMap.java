package org.kingdoms.services.maps.bluemap;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.math.Shape;
import org.bukkit.World;
import org.kingdoms.platform.bukkit.adapters.BukkitAdapter;
import org.kingdoms.server.location.Vector2;
import org.kingdoms.server.location.Vector3;
import org.kingdoms.server.location.Vector3Location;
import org.kingdoms.services.maps.abstraction.MapAPI;
import org.kingdoms.services.maps.abstraction.markers.IconMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;
import org.kingdoms.services.maps.abstraction.markers.MarkerZoom;
import org.kingdoms.services.maps.abstraction.markersets.MarkerType;
import org.kingdoms.services.maps.abstraction.outliner.Polygon;
import org.kingdoms.utils.versionsupport.VersionSupport;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public final class ServiceBlueMap implements MapAPI {
    protected static final String LABEL_POPUP = ".bm-marker-labelpopup";
    protected static final int ICON_SIZE = 32;

    private final Map<UUID, Map<MarkerType, MarkerSet>> markerSets = new HashMap<>(3);
    private final Logger logger;

    public static void createWhenAvailable(Logger logger, Runnable runnable) {
        BlueMapAPI.onEnable(api -> {
            logger.info("BlueMap's API is now available. Loading markers...");
            runnable.run();
        });
    }

    public ServiceBlueMap(Logger logger) {
        this.logger = logger;
        logger.info("Initializing BlueMap support...");
    }

    public static BlueMapAPI getAPI() {
        return BlueMapAPI.getInstance().orElseThrow(() -> new NullPointerException("BlueMap's API is disabled."));
    }

    private static BufferedImage resize(BufferedImage image, int newW, int newH) {
        Image scaled = image.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage result = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();

        return result;
    }

    /**
     * https://github.com/BlueMap-Minecraft/BlueMap/blob/31910382b02e5ef9865bf83d862fecb5714a5bc1/BlueMapCommon/src/main/java/de/bluecolored/bluemap/common/api/marker
     * /MarkerAPIImpl.java#L73
     * https://github.com/BlueMap-Minecraft/BlueMapAPI/wiki
     * https://discord.com/channels/665868367416131594/751804128749027421/944147878270631986
     *
     * <quote>
     * getMarkerAPI() and MarkerAPI#save() are IO operations, but you can't hold the reference to it too long,
     * because it gets invalid. E.g. as soon as bluemap reloads.
     * <p>
     * MarkerAPI markerapi = bluemapapi.getMarkerAPI();
     * //update all markers you need to update in a batch
     * markerapi.save()
     * //throw markerapi and all other marker(set) instances away
     * <p>
     * <p>
     * and next time you need to change something do all that again
     * and the most optimal way would be to do this in a separate thread (because of IO)
     * </quote>
     */
    protected MarkerSet getMarkerSet(MarkerType markerType, World world) {
        Map<MarkerType, MarkerSet> markerSets = this.markerSets.get(world.getUID());
        MarkerSet markerSet = null;

        if (markerSets != null) {
            markerSet = markerSets.get(markerType);
        } else {
            markerSets = new IdentityHashMap<>(5);
            this.markerSets.put(world.getUID(), markerSets);
        }

        if (markerSet != null) return markerSet;

        Optional<BlueMapWorld> map = getAPI().getWorld(world);
        if (!map.isPresent()) return null; // Handled by isEnabledInWorld

        markerSet = new MarkerSet(
                markerType.getMarkerSettings().label,
                markerType.getMarkerSettings().showControls,
                markerType.getMarkerSettings().hideByDefault
        );
        markerSet.setSorting(markerType.getMarkerSettings().sorting);

        for (BlueMapMap blueMapMap : map.get().getMaps()) {
            blueMapMap.getMarkerSets().put(markerType.getId(), markerSet);
        }

        markerSets.put(markerType, markerSet);
        return markerSet;
    }

    @Override
    public Optional<LandMarker> getLandMarker(MarkerType markerType, String id, World world) {
        MarkerSet markerSet = getMarkerSet(markerType, world);
        if (markerSet == null) return Optional.empty();

        Marker marker = markerSet.getMarkers().get(id);
        if (marker == null) return Optional.empty();

        return Optional.of(new LandMarkerBlueMap(markerSet, marker, id));
    }

    @Override
    public List<LandMarker> createLandMarkers(MarkerType markerType, String id, String label, World world, List<Polygon> polygons, LandMarkerSettings settings) {
        List<LandMarker> markerOps = new ArrayList<>(polygons.size());

        // Delete markers if current multipoly size is less than old multipoly size
        MarkerSet markerSet = Objects.requireNonNull(getMarkerSet(markerType, world), () -> "Null marker set for world " + world.getName() + " handling id " + id);
        for (int i = 0; i < polygons.size(); i++) {
            final Polygon polygon = polygons.get(i);

            // ------------------------------
            // Author's response:
            //   Since BlueMap is a 3D map, these markers are sorted by distance to the camera.
            //   The distance is calculated based on the markers position property.
            //   closer markers will be rendered on top. So you can e.g. increase the y-position
            //   value of the red markers to make them appear on top (if viewed from top-down)
            //
            //   the position is used to control the render-order .. but there is still a depth-test
            //   done when drawing the actual marker-geometry themselves, so parts of the markers that
            //   are closer will be rendered on top ..
            // ------------------------------
            // In order to fix neighbouring borders of two markers we would have to shrink one.
            // The process of doing that is going to be pretty complicated, we have to modify our chunk size in
            // the poly point finder. I ain't doing that...
            final de.bluecolored.bluemap.api.math.Shape shape = new de.bluecolored.bluemap.api.math.Shape(pointsToVecs(polygon.getPoints()));
            ExtrudeMarker marker = new ExtrudeMarker(label + "'s land", shape,
                    VersionSupport.getMinWorldHeight(world),
                    world.getMaxHeight() + (settings.getPriority() == null ? 0 : settings.getPriority() * 2) // *2 just to be safe
            );

            polygon.getNegativeSpace().forEach(x -> marker.getHoles().add(new Shape(pointsToVecs(x))));
            String key = id + '_' + i;
            markerSet.getMarkers().put(key, marker);
            markerOps.add(new LandMarkerBlueMap(markerSet, marker, key));
        }

        return markerOps;
    }

    @Override
    public String getPopupContainerSelector() {
        return LABEL_POPUP;
    }

    @Override
    public MarkerZoom translateZoom(int min, int max) {
        return translateZoom0(min, max);
    }

    protected static MarkerZoom translateZoom0(final int min, final int max) {
        // 400 is the max zoom level??? if min zoom is set to 400 you cant see anything if you're fully zoomed in
        // The min zoom level  can technically go up to 200000
        // This distance seems to be in blocks just like the options in webapp.conf?
        // We certainly have to limit ourselves here because of the opacity fading effect.
        // ------------------------------
        // Author's response:
        //   The min/max values are also based on the distance to the camera position in 3d space ..
        //   the fade in/out is a small margin centered around those min/max points which can't be controlled via the API..
        //
        //   A big caveat of those min/max distance values for markers is that they don't work very well in bluemaps flat
        //   (isometric/top-down) view, because in the flat-view the camera technically never gets any closer than ~300 blocks.
        //   When zooming further in the flat-view camera is no longer moving towards the ground, instead it actually does a zoom ..
        //   this is to avoid clipping issues with vertical terrain, but it unfortunately breaks the distance-calulation at this point.
        //   I want to fix that at some point, but right now this is a thing and means that min/max only works for greater
        //   distances in the flat-view.
        //
        //   In perspective view these min/max values should work consistently based on distance in blocks :)
        // ------------------------------
        double blueMin;
        if (max <= 0) blueMin = 0;
        else blueMin = MarkerZoom.valueFromPercent(400, 3000, 100 - max);

        double blueMax;
        if (min <= 0) blueMax = Integer.MAX_VALUE;
        else blueMax = MarkerZoom.valueFromPercent(400, 3000, 100 - min);

        return MarkerZoom.of(blueMin, blueMax);
    }

    private static Vector2d pointToVec(Vector2 point) {
        return new Vector2d(point.getX(), point.getZ());
    }

    private static Vector2d[] pointsToVecs(Collection<Vector2> points) {
        return points.stream()
                .map(ServiceBlueMap::pointToVec)
                .toArray(Vector2d[]::new);
    }

    protected static Vector3 adapt(Vector3d vector3d) {
        return Vector3.of(vector3d.getX(), vector3d.getY(), vector3d.getZ());
    }

    @Override
    public boolean isEnabledInWorld(org.kingdoms.server.location.World world) {
        return getAPI().getWorld(world.getId()).isPresent();
    }

    @Override
    public void removeIconMarker(MarkerType markerType, String id, Vector3Location location) {
        MarkerSet markerSet = getMarkerSet(markerType, BukkitAdapter.adapt(location.getWorld()));
        if (markerSet == null) return;
        markerSet.remove(id);
    }

    @Override
    public IconMarker updateOrAddIcon(MarkerType markerType, String id, LandMarkerSettings settings, Vector3Location location, Object icon) {
        MarkerSet markerSet = getMarkerSet(markerType, BukkitAdapter.adapt(location.getWorld()));
        if (markerSet == null) return null;

        POIMarker poi = new POIMarker(settings.getClickDescription(),
                new Vector3d(location.getX(), location.getY(), location.getZ()));
        poi.setIcon((String) icon, ICON_SIZE / 2, ICON_SIZE / 2);

        poi.setMinDistance(settings.getZoomMin());
        poi.setMaxDistance(settings.getZoomMax());

        markerSet.getMarkers().put(id, poi);
        return new IconMarkerBlueMap(markerSet, poi, id);
    }

    @Override
    public void removeEverything() {
        getAPI().getMaps().forEach(map -> {
            Map<String, MarkerSet> markerSets = map.getMarkerSets();
            for (MarkerType markerType : MarkerType.getMarkerTypes()) {
                markerSets.remove(markerType.getId());
            }
        });
        this.markerSets.clear();
    }

    @Override
    public String createImage(BufferedImage image, String imageFileName) {
        // https://github.com/BlueMap-Minecraft/BlueMap/blob/6bf4291779f3f7d5baf16fa43884688a5f3e1342/BlueMapCommon/src/main/java/de/bluecolored/bluemap/common/api/WebAppImpl.java#L79-L98
        // Copied version of getAPI().getWebApp().createImage(img, id) since it was deprecated.
        Path webRoot = getAPI().getWebApp().getWebRoot().toAbsolutePath();
        String separator = webRoot.getFileSystem().getSeparator();

        Path imageRootFolder = webRoot.resolve("data").resolve("images");
        return MapAPI.createImage(image, imageFileName, webRoot, imageRootFolder, separator);
    }

    @Override
    public Object updateOrRegisterIcon(String id, Path path) {
        try {
            BufferedInputStream is = new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ));
            BufferedImage img = ImageIO.read(is);
            img = resize(img, ICON_SIZE, ICON_SIZE);
            return createImage(img, id);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static de.bluecolored.bluemap.api.math.Color toBlueColor(java.awt.Color color) {
        return new de.bluecolored.bluemap.api.math.Color(color.getRGB());
    }
}
