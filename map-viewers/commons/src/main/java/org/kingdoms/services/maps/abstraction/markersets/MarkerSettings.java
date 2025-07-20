package org.kingdoms.services.maps.abstraction.markersets;

import java.util.Objects;
import java.util.function.Function;

public final class MarkerSettings {
    public boolean showControls, hideByDefault, persistent;
    public int priority, zIndex, sorting;
    public String label;
    public Function<org.kingdoms.server.location.World, Boolean> isWorldDisabled;

    public MarkerSettings label(String label) {
        this.label = Objects.requireNonNull(label, "Marker label is null");
        return this;
    }

    public MarkerSettings showControls(boolean showControls) {
        this.showControls = showControls;
        return this;
    }

    public MarkerSettings hideByDefault(boolean hideByDefault) {
        this.hideByDefault = hideByDefault;
        return this;
    }

    public MarkerSettings persistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    public MarkerSettings priority(int priority) {
        this.priority = priority;
        return this;
    }

    public MarkerSettings zIndex(int zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    public MarkerSettings sorting(int sorting) {
        this.sorting = sorting;
        return this;
    }

    public MarkerSettings disabledWorlds(Function<org.kingdoms.server.location.World, Boolean> isWorldDisabled) {
        this.isWorldDisabled = isWorldDisabled;
        return this;
    }
}
