package org.kingdoms.services.maps.bluemap;

import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.server.location.Vector3;
import org.kingdoms.services.maps.abstraction.markers.IconMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;
import org.kingdoms.services.maps.abstraction.markers.MarkerZoom;

public class IconMarkerBlueMap extends MarkerBlueMap<POIMarker> implements IconMarker {
    protected IconMarkerBlueMap(MarkerSet markerSet, POIMarker marker, String id) {
        super(markerSet, marker, id);
    }

    @Override
    public void setSettings(LandMarkerSettings settings) {
        if (settings.getPriority() != null) marker.setSorting(settings.getPriority());

        MarkerZoom zoom = ServiceBlueMap.translateZoom0(settings.getZoomMin(), settings.getZoomMax());
        marker.setMinDistance(zoom.getMin().doubleValue());
        marker.setMaxDistance(zoom.getMax().doubleValue());
    }

    @NotNull
    @Override
    public Vector3 getLocation() {
        return ServiceBlueMap.adapt(marker.getPosition());
    }
}
