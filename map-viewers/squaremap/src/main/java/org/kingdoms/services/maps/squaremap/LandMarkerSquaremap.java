package org.kingdoms.services.maps.squaremap;

import org.kingdoms.services.maps.abstraction.markers.LandMarker;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.marker.Marker;

public class LandMarkerSquaremap extends MarkerSquaremap<Marker> implements LandMarker {
    public LandMarkerSquaremap(Marker marker, Key key, SimpleLayerProvider layer) {
        super(marker, key, layer);
    }
}
