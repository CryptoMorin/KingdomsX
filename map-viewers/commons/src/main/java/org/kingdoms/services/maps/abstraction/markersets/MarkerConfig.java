package org.kingdoms.services.maps.abstraction.markersets;

import org.kingdoms.config.implementation.KeyedYamlConfigAccessor;
import org.kingdoms.utils.config.ConfigPath;

import java.util.Locale;

public enum MarkerConfig {
    DISABLED,
    PERSISTENT_DATA,
    LABEL,
    SORTING,
    PRIORITY,
    Z_INDEX,
    SPECIAL_FLAG,
    HIDE_BY_DEFAULT,
    SHOW_CONTROLS,

    ZOOM_MAX(1),
    ZOOM_MIN(1),

    FILL_OPACITY(1),
    FILL_COLOR(1),
    FILL_FORCE(1),

    LINE_OPACITY(1),
    LINE_COLOR(1),
    LINE_WEIGHT(1),
    LINE_FORCE(1),

    INVASION_FILL_OPACITY(1, 2),
    INVASION_FILL_COLOR(1, 2),

    INVASION_LINE_OPACITY(1, 2),
    INVASION_LINE_COLOR(1, 2),
    INVASION_LINE_WEIGHT(1, 2),

    DESCRIPTIONS_FLAG(1),
    DESCRIPTIONS_CLICK(1),
    DESCRIPTIONS_HOVER(1),
    DESCRIPTIONS_MEMBERS(1),
    DESCRIPTIONS_MEMBERS_ETC(1),
    DESCRIPTIONS_ALLIES(1),
    DESCRIPTIONS_TRUCES(1),
    DESCRIPTIONS_ENEMIES(1),

    ICONS_ZOOM_MAX(1, 2),
    ICONS_ZOOM_MIN(1, 2);

    private final ConfigPath option;

    MarkerConfig() {
        this.option = new ConfigPath(this.name().toLowerCase(Locale.ENGLISH).replace('_', '-'));
    }

    MarkerConfig(int... grouped) {
        this.option = new ConfigPath(this.name(), grouped);
    }

    public KeyedYamlConfigAccessor getManager(MarkerType markerType) {
        if (markerType.getConfigAdapter().isPresent()) {
            return new KeyedYamlConfigAccessor(markerType.getConfigAdapter().get(), option);
        } else {
            return new KeyedYamlConfigAccessor(markerType.getConfig(), null, option);
        }
    }
}
