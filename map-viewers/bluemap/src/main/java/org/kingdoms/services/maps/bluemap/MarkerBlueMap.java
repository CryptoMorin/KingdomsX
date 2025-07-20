package org.kingdoms.services.maps.bluemap;

import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import org.jetbrains.annotations.NotNull;

abstract class MarkerBlueMap<T extends Marker> implements org.kingdoms.services.maps.abstraction.markers.Marker {
    protected final MarkerSet markerSet;
    protected final T marker;
    protected final String id;

    protected MarkerBlueMap(MarkerSet markerSet, T marker, String id) {
        this.markerSet = markerSet;
        this.marker = marker;
        this.id = id;
    }

    @Override
    public final void delete() {
        markerSet.getMarkers().remove(id);
    }

    @NotNull
    @Override
    public final String id() {
        return id;
    }
}
