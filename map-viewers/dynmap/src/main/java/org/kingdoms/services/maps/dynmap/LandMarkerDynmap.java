package org.kingdoms.services.maps.dynmap;

import com.cryptomorin.xseries.reflection.ReflectiveNamespace;
import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.jvm.classes.DynamicClassHandle;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerSet;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.main.KLogger;
import org.kingdoms.services.maps.abstraction.MapAPI;
import org.kingdoms.services.maps.abstraction.markers.LandMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;
import org.kingdoms.services.maps.abstraction.markers.MarkerZoom;
import org.kingdoms.utils.ColorUtils;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LandMarkerDynmap implements LandMarker {
    private static final MethodHandle
            AreaMarker_setDescription,
            MarkerAPIImpl_areaMarkerUpdated,
            MarkerAPIImpl_saveMarkers;
    private static final Object MarkerAPIImpl$MarkerUpdate_UPDATED;

    private final AreaMarker marker;
    private final MarkerSet markerSet;

    static {
        ReflectiveNamespace ns = XReflection.namespaced();

        AreaMarker_setDescription = ns
                .classHandle("package org.dynmap.markers.impl;" +
                        "class AreaMarkerImpl implements AreaMarker, EnterExitMarker")
                .field("private String desc").setter()
                .reflectOrNull();


        DynamicClassHandle MarkerAPIImpl = ns
                .classHandle("package org.dynmap.markers.impl;" +
                        "public class MarkerAPIImpl implements MarkerAPI");

        // enum MarkerUpdate { CREATED, UPDATED, DELETED };
        DynamicClassHandle enumMarkerUpdate = MarkerAPIImpl
                .inner("enum MarkerUpdate");
        MarkerAPIImpl$MarkerUpdate_UPDATED = enumMarkerUpdate
                .field().getter().returns(enumMarkerUpdate).makeAccessible().named("UPDATED").getStatic();

        MarkerAPIImpl_areaMarkerUpdated = MarkerAPIImpl
                .method("static void areaMarkerUpdated(AreaMarkerImpl marker, MarkerUpdate update)")
                .reflectOrNull();

        MarkerAPIImpl_saveMarkers = MarkerAPIImpl
                .method("static void saveMarkers()")
                .reflectOrNull();

        Map<String, Object> hacks = new HashMap<>();
        hacks.put("AreaMarker_setDescription", AreaMarker_setDescription);
        hacks.put("MarkerAPIImpl_areaMarkerUpdated", MarkerAPIImpl_areaMarkerUpdated);
        hacks.put("MarkerAPIImpl$MarkerUpdate_UPDATED", MarkerAPIImpl$MarkerUpdate_UPDATED);
        hacks.put("MarkerAPIImpl_saveMarkers", MarkerAPIImpl_saveMarkers);

        if (hacks.values().stream().anyMatch(Objects::isNull)) {
            KLogger.warn("Your current Dynmap version doesn't support one of the HTML hacks:");
            hacks.forEach((k, v) -> KLogger.warn("  - " + k));
        }
    }

    public LandMarkerDynmap(AreaMarker marker, MarkerSet markerSet) {
        this.marker = Objects.requireNonNull(marker);
        this.markerSet = markerSet;
    }

    @Override
    public void delete() {
        marker.deleteMarker();
    }

    @Override
    public void setSettings(LandMarkerSettings settings) {
        // I have no clue how and when this happens, but since all methods used here depend on the
        // marker set to be used, it'll throw the NullPointerException from Dynmap.
        if (marker.getMarkerSet() == null) marker.setMarkerSet(markerSet);
        Objects.requireNonNull(marker.getMarkerSet(), () -> "Dynmap marker set is unavailable for land marker " + marker + " with supposed MarkerSet is " + markerSet + " this is an issue with Dynmap");

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
        // No other map software (BlueMap, Squaremap and Pl3xMap) has this restriction.
        // We already escape the whole value that comes from placeholders inside our HTML text processor.

        //     @Override
        //     public void setDescription(String desc) {
        //         if(markerset == null) return;
        //         desc = Client.sanitizeHTML(desc);
        //         if((this.desc == null) || (this.desc.equals(desc) == false)) {
        //             this.desc = desc;
        //             MarkerAPIImpl.areaMarkerUpdated(this, MarkerUpdate.UPDATED);
        //             if(ispersistent)
        //                 MarkerAPIImpl.saveMarkers();
        //         }
        //     }
        String clickDescription = MapAPI.replaceSelector(settings.getClickDescription(), ServiceDynmap.LEAFLET_POPUP_PANES);
        if (AreaMarker_setDescription != null) {
            try {
                // Can't invokeExact because we don't have access to AreaMarkerImpl
                AreaMarker_setDescription.invoke(marker, clickDescription);

                // MarkerAPIImpl.areaMarkerUpdated(this, MarkerUpdate.UPDATED);
                if (MarkerAPIImpl_areaMarkerUpdated != null && MarkerAPIImpl$MarkerUpdate_UPDATED != null)
                    MarkerAPIImpl_areaMarkerUpdated.invoke(marker, MarkerAPIImpl$MarkerUpdate_UPDATED);

                if (MarkerAPIImpl_saveMarkers != null && marker.isPersistentMarker())
                    MarkerAPIImpl_saveMarkers.invokeExact();
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
