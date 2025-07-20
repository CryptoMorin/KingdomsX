package org.kingdoms.services.maps.abstraction.markersets;

import org.kingdoms.constants.namespace.NamespacedRegistry;

public class MarkerTypeRegistry extends NamespacedRegistry<MarkerType> {
    public static final MarkerTypeRegistry INSTANCE = new MarkerTypeRegistry();

    private MarkerTypeRegistry() {}
}
