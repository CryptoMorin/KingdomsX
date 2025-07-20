package org.kingdoms.services.maps.abstraction;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.kingdoms.server.location.Vector3Location;
import org.kingdoms.services.maps.abstraction.markers.IconMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;
import org.kingdoms.services.maps.abstraction.markers.MarkerZoom;
import org.kingdoms.services.maps.abstraction.markersets.MarkerType;
import org.kingdoms.services.maps.abstraction.outliner.Polygon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface MapAPI {
    Optional<LandMarker> getLandMarker(MarkerType markerType, String id, World world);

    /**
     * @param settings This parameter was merely passed so immutable properties can be set from it.
     *                 Specifically for BlueMap that requires some more extra calculations to get the "z-index" working.
     */
    List<LandMarker> createLandMarkers(MarkerType markerType, String id, String label, World world, List<Polygon> polygon, LandMarkerSettings settings);

    boolean isEnabledInWorld(org.kingdoms.server.location.World world);

    default void sendMessage(Player player, String message) {}

    IconMarker updateOrAddIcon(MarkerType markerType, String id, LandMarkerSettings settings, Vector3Location location, Object icon);

    String getPopupContainerSelector();

    void removeIconMarker(MarkerType markerType, String id, Vector3Location location);

    Object updateOrRegisterIcon(String id, Path path);

    /**
     * Some maps only allow certain characters for their marker ID,
     * this replaces the illegal characters with another allowed character.
     */
    default String normalizeId(String id) {return id;}

    MarkerZoom translateZoom(int min, int max);

    /**
     * For cleanup purposes.
     */
    void removeEverything();

    /**
     * Sends the signal that the map should commit all changes.
     * No map currently uses this feature.
     */
    default void update() {}

    /**
     * @param image    The image to save.
     * @param fileName The file name including the relative folders.
     * @return The string representing the final saved image location. Should be usable in {@code <img href="...">}.
     */
    String createImage(BufferedImage image, String fileName);

    static String createImage(BufferedImage image, String fileName,
                              Path rootFolder, Path imagesFolder,
                              String pathSeparator) {
        String format = "png";
        Path imagePath;
        {
            String finalPath = fileName;
            if (pathSeparator != null) finalPath = fileName.replace("/", pathSeparator);
            imagePath = imagesFolder.resolve("kingdoms").resolve(finalPath + '.' + format).toAbsolutePath();
        }

        try {
            Files.createDirectories(imagePath.getParent());
            Files.deleteIfExists(imagePath);
            Files.createFile(imagePath);

            if (!ImageIO.write(image, format, imagePath.toFile()))
                throw new IOException("The format '" + format + "' is not supported!");
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to save image: " + fileName, ex);
        }

        String relativized = rootFolder.relativize(imagePath).toString();
        if (pathSeparator != null) relativized = relativized.replace(pathSeparator, "/");
        return relativized;
    }

    static String replaceSelector(String description, String selector) {
        if (description == null || description.isEmpty()) return description;
        return description.replace("%popup_container_selector%", selector);
    }
}
