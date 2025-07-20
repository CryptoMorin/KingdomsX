package org.kingdoms.services.maps.pl3xmap;

import com.cryptomorin.xseries.reflection.XReflection;
import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.markers.marker.Icon;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.server.location.Vector3;
import org.kingdoms.services.maps.abstraction.markers.IconMarker;

import java.lang.invoke.MethodHandle;

public class IconMarkerPl3xMap extends MarkerPl3xMap<Icon> implements IconMarker {
    private static final MethodHandle Point_x = XReflection.of(Point.class)
            .method("public int x()")
            .unreflect();
    private static final MethodHandle Point_z = XReflection.of(Point.class)
            .method("public int z()")
            .unreflect();

    public IconMarkerPl3xMap(Icon marker, String key, SimpleLayer layer) {
        super(marker, key, layer);
    }

    @NotNull
    @Override
    public Vector3 getLocation() {
        // Using the Point class as a variable type will trigger:
        //   class file for java.lang.Record not found
        Object point = marker.getPoint();
        try {
            return Vector3.of((int) Point_x.invoke(point), 0, (int) Point_z.invoke(point));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
