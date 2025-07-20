package org.kingdoms.services.maps.dynmap;

import com.cryptomorin.xseries.reflection.XReflection;
import org.dynmap.markers.AreaMarker;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.main.KLogger;
import org.kingdoms.services.maps.abstraction.MapAPI;
import org.kingdoms.services.maps.abstraction.markers.LandMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;
import org.kingdoms.services.maps.abstraction.markers.MarkerZoom;
import org.kingdoms.utils.ColorUtils;

import java.lang.invoke.MethodHandle;
import java.util.Objects;

public class LandMarkerDynmap implements LandMarker {
    private final AreaMarker marker;
    private final MethodHandle AreaMarker_setDescription = XReflection.classHandle()
            .inPackage("org.dynmap.markers.impl")
            .named("AreaMarkerImpl")
            .field("private String desc").setter()
            .reflectOrNull();

    public LandMarkerDynmap(AreaMarker marker) {
        this.marker = Objects.requireNonNull(marker);
    }

    @Override
    public void delete() {
        marker.deleteMarker();
    }

    @Override
    public void setSettings(LandMarkerSettings settings) {
        // https://github.com/webbukkit/dynmap/blob/v3.0/DynmapCoreAPI/src/main/java/org/dynmap/markers/EnterExitMarker.java
        // WorldGuard style titles. We don't need this, it's implemented into Kingdoms plugin itself.
        // marker.setFarewellText("Title", "Subtitle");
        // marker.setGreetingText("Title", "Subtitle");

        marker.setFillStyle(settings.getFillColor().getAlpha() / 255D, ColorUtils.toHex(settings.getFillColor()));
        marker.setLineStyle(settings.getLineWidth(), settings.getLineColor().getAlpha() / 255D, ColorUtils.toHex(settings.getLineColor()));
        marker.setBoostFlag(settings.getSpecialFlag());

        // Dynmap uses an HTML sanitizer because of "security concerns" and they have no
        // intention of making this work at all...
        // https://github.com/webbukkit/dynmap/pull/3338
        // https://github.com/webbukkit/dynmap/issues/3987
        // https://github.com/webbukkit/dynmap/blob/003cad5dc280b68eb675dc7683a87b0ee7b48b58/DynmapCore/src/main/java/org/dynmap/markers/impl/AreaMarkerImpl.java#L296-L305
        // No other map software (BlueMap, Squaremap and Pl3xMap) have this restriction.
        // We already escape the whole value that comes from placeholders inside our HTML text processor.
        String clickDescription = MapAPI.replaceSelector(settings.getClickDescription(), ServiceDynmap.LEAFLET_POPUP_PANES);

        if (AreaMarker_setDescription != null) {
            try {
                AreaMarker_setDescription.invoke(marker, clickDescription);
            } catch (Throwable e) {
                if (KLogger.isDebugging()) e.printStackTrace();
                else KLogger.warn("Failed to set Dynmap's unsafe description: " + e.getMessage());
                marker.setDescription(clickDescription);
            }
        } else {
            marker.setDescription(clickDescription);
        }

        MarkerZoom zoom = ServiceDynmap.translateZoom0(settings.getZoomMin(), settings.getZoomMax());
        marker.setMinZoom(zoom.getMin().intValue());
        marker.setMaxZoom(zoom.getMax().intValue());
    }

    @NotNull
    @Override
    public String id() {
        return marker.getUniqueMarkerID();
    }
}
