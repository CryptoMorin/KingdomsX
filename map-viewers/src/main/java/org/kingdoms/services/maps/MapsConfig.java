package org.kingdoms.services.maps;

import org.kingdoms.config.implementation.KeyedYamlConfigAccessor;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.utils.config.ConfigPath;
import org.kingdoms.utils.config.adapters.YamlResource;

import java.util.Locale;

public enum MapsConfig {
    UPDATE_INTERVAL,

    BANNERS_SCALING(1),
    ICONS,
    ICONS_HOME_USE_BANNER(1, 2),
    ICONS_ENABLED(1), INVASION_LINE_COLOR;

    private final ConfigPath option;

    MapsConfig() {
        this.option = new ConfigPath(this.name().toLowerCase(Locale.ENGLISH).replace('_', '-'));
    }

    MapsConfig(int... grouped) {
        this.option = new ConfigPath(this.name(), grouped);
    }

    protected static final YamlResource WEB_MAP = new YamlResource(
            MapViewerAddon.get(),
            Kingdoms.getFolder().resolve("maps").resolve("map.yml").toFile(),
            "maps/map.yml"
    );

    public KeyedYamlConfigAccessor getManager() {
        return new KeyedYamlConfigAccessor(WEB_MAP, option);
    }
}
