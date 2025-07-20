package org.kingdoms.services.maps.dynmap;

import org.jetbrains.annotations.NotNull;
import org.kingdoms.server.location.Vector3;
import org.kingdoms.services.maps.abstraction.markers.IconMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;
import org.kingdoms.services.maps.abstraction.markers.MarkerZoom;

public class IconMarkerDynmap implements IconMarker {
    /**
     * Dynmap hides its icon marker implementation behind the generic Marker object.
     */
    private final org.dynmap.markers.Marker marker;

    public IconMarkerDynmap(org.dynmap.markers.Marker marker) {this.marker = marker;}

    @Override
    public void delete() {
        marker.deleteMarker();
    }

    @Override
    public void setSettings(LandMarkerSettings settings) {
        marker.setDescription(settings.getClickDescription());

        MarkerZoom zoom = ServiceDynmap.translateZoom0(settings.getZoomMin(), settings.getZoomMax());
        marker.setMinZoom(zoom.getMin().intValue());
        marker.setMaxZoom(zoom.getMax().intValue());
    }

    @NotNull
    @Override
    public String id() {
        return marker.getUniqueMarkerID();
    }

    @NotNull
    @Override
    public Vector3 getLocation() {
        return Vector3.of(marker.getX(), marker.getY(), marker.getZ());
    }
}
