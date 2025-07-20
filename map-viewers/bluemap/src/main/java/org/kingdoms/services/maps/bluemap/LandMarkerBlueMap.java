package org.kingdoms.services.maps.bluemap;

import de.bluecolored.bluemap.api.markers.*;
import org.kingdoms.services.maps.abstraction.MapAPI;
import org.kingdoms.services.maps.abstraction.markers.LandMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;
import org.kingdoms.services.maps.abstraction.markers.MarkerZoom;

import static org.kingdoms.services.maps.bluemap.ServiceBlueMap.toBlueColor;

public class LandMarkerBlueMap extends MarkerBlueMap<Marker> implements LandMarker {
    protected LandMarkerBlueMap(MarkerSet markerSet, Marker marker, String id) {
        super(markerSet, marker, id);
    }

    @Override
    public void setSettings(LandMarkerSettings settings) {
        if (marker instanceof ShapeMarker) {
            ShapeMarker shapeMarker = (ShapeMarker) marker;
            shapeMarker.setFillColor(toBlueColor(settings.getFillColor()));
            shapeMarker.setLineColor(toBlueColor(settings.getLineColor()));
            shapeMarker.setLineWidth(settings.getLineWidth());
            shapeMarker.setDepthTestEnabled(settings.getSpecialFlag());
        } else if (marker instanceof ExtrudeMarker) {
            ExtrudeMarker extrudeMarker = (ExtrudeMarker) marker;
            extrudeMarker.setFillColor(toBlueColor(settings.getFillColor()));
            extrudeMarker.setLineColor(toBlueColor(settings.getLineColor()));
            extrudeMarker.setLineWidth(settings.getLineWidth());

            // If the depth-test is disabled, you can see the marker fully through all objects on the map.
            // If it is enabled, you'll only see the marker when it is not behind anything.
            extrudeMarker.setDepthTestEnabled(settings.getSpecialFlag());
        } else if (marker instanceof LineMarker) {
            LineMarker lnMarker = (LineMarker) marker;
            lnMarker.setLineColor(toBlueColor(settings.getLineColor()));
            lnMarker.setLineWidth(settings.getLineWidth());
            lnMarker.setDepthTestEnabled(settings.getSpecialFlag());
        }

        if (marker instanceof ObjectMarker) {
            ObjectMarker objMarker = (ObjectMarker) marker;
            String clickDescription = MapAPI.replaceSelector(settings.getClickDescription(), ServiceBlueMap.LABEL_POPUP);
            objMarker.setDetail(clickDescription);
        }

        if (marker instanceof DistanceRangedMarker) {
            DistanceRangedMarker rangedMarker = (DistanceRangedMarker) marker;

            MarkerZoom zoom = ServiceBlueMap.translateZoom0(settings.getZoomMin(), settings.getZoomMax());
            rangedMarker.setMinDistance(zoom.getMin().doubleValue());
            rangedMarker.setMaxDistance(zoom.getMax().doubleValue());
        }
    }
}
