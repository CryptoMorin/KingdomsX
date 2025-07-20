package org.kingdoms.services.maps.pl3xmap;

import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.markers.marker.MultiPolygon;
import org.kingdoms.services.maps.abstraction.markers.LandMarker;

public class LandMarkerPl3xMap extends MarkerPl3xMap<MultiPolygon> implements LandMarker {
    public LandMarkerPl3xMap(MultiPolygon marker, String key, SimpleLayer layer) {
        super(marker, key, layer);
    }
}
