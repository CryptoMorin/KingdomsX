package org.kingdoms.services.maps.squaremap;

import org.jetbrains.annotations.NotNull;
import org.kingdoms.services.maps.abstraction.MapAPI;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

import java.util.Objects;

public class MarkerSquaremap<T extends Marker> implements org.kingdoms.services.maps.abstraction.markers.Marker {
    protected final T marker;
    private final SimpleLayerProvider layer;
    private final Key key;

    public MarkerSquaremap(T marker, Key key, SimpleLayerProvider layer) {
        this.marker = Objects.requireNonNull(marker);
        this.key = key;
        this.layer = layer;
    }

    @Override
    public final void delete() {
        layer.removeMarker(key);
    }

    public final void setSettings(LandMarkerSettings settings) {
        // Since we share this for both land and icons, some properties might be null, specially for icons.
        MarkerOptions.Builder builder = MarkerOptions.builder();
        if (settings.getFillColor() != null)
            builder.fill(true).fillColor(settings.getFillColor());

        if (settings.getLineColor() != null)
            builder.stroke(true).strokeColor(settings.getLineColor()).strokeWeight(settings.getLineWidth());

        marker.markerOptions(builder
                .clickTooltip(MapAPI.replaceSelector(settings.getClickDescription(), ServiceSquaremap.POPUP_CSS_SELECTOR))
                .hoverTooltip(MapAPI.replaceSelector(settings.getHoverDescription(), ServiceSquaremap.POPUP_CSS_SELECTOR))
                .build()
        );
    }

    @NotNull
    @Override
    public final Object id() {
        return key;
    }
}
