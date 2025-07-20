package org.kingdoms.services.maps.squaremap;

import org.jetbrains.annotations.NotNull;
import org.kingdoms.server.location.Vector3;
import org.kingdoms.services.maps.abstraction.markers.IconMarker;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.marker.Icon;

public class IconMarkerSquaremap extends MarkerSquaremap<Icon> implements IconMarker {
    public IconMarkerSquaremap(Icon marker, Key key, SimpleLayerProvider layer) {
        super(marker, key, layer);
    }

    @NotNull
    @Override
    public Vector3 getLocation() {
        Point point = marker.point();
        return Vector3.of(point.x(), 0, point.z());
    }
}
