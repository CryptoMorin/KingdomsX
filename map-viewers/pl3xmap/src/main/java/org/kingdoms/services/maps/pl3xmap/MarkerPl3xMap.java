package org.kingdoms.services.maps.pl3xmap;

import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.markers.marker.Marker;
import net.pl3x.map.core.markers.option.Options;
import net.pl3x.map.core.markers.option.Popup;
import net.pl3x.map.core.markers.option.Tooltip;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.services.maps.abstraction.MapAPI;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;

import java.util.Objects;

public class MarkerPl3xMap<T extends Marker<T>> implements org.kingdoms.services.maps.abstraction.markers.Marker {
    protected final T marker;
    private final SimpleLayer layer;
    private final String key;

    public MarkerPl3xMap(T marker, String key, SimpleLayer layer) {
        this.marker = Objects.requireNonNull(marker);
        this.key = key;
        this.layer = layer;
    }

    @Override
    public final void delete() {
        layer.removeMarker(key);
    }

    public final void setSettings(LandMarkerSettings settings) {
        // https://billygalbreath.github.io/Pl3xMap/net/pl3x/map/markers/option/Options.Builder.html#fillColor(java.lang.Integer)
        // Why is it an integer?? What is the integer supposed to be? RGB?

        // Since we share this for both land and icons, some properties might be null, specially for icons.
        Options.Builder builder = new Options.Builder();
        if (settings.getFillColor() != null)
            builder.fill(true).fillColor(settings.getFillColor().getRGB());

        if (settings.getLineColor() != null)
            builder.stroke(true).strokeColor(settings.getLineColor().getRGB()).strokeWeight(settings.getLineWidth());

        marker.setOptions(builder
                .tooltip(new Tooltip(MapAPI.replaceSelector(settings.getHoverDescription(), ServicePl3xMap.POPUP_CSS_SELECTOR)))
                .popup(new Popup(MapAPI.replaceSelector(settings.getClickDescription(), ServicePl3xMap.POPUP_CSS_SELECTOR)))
                .build()
        );
    }

    @NotNull
    @Override
    public final String id() {
        return key;
    }
}
