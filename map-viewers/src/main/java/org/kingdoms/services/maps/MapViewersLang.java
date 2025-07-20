package org.kingdoms.services.maps;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.config.annotations.Path;
import org.kingdoms.locale.LanguageEntry;
import org.kingdoms.locale.messenger.DefinedMessenger;

public enum MapViewersLang implements DefinedMessenger {
    COMMAND_ADMIN_DYNMAP_DESCRIPTION("{$s}Perform different actions for Dynmap.", 1, 2, 3),
    COMMAND_ADMIN_DYNMAP_RENDERING("{$p}Rendering...", 1, 2, 3),
    COMMAND_ADMIN_DYNMAP_USAGE("{$e}Usage{$colon} {$es}/k admin dynmap <fullrender/update/remove>", 1, 2, 3),
    COMMAND_ADMIN_DYNMAP_NOT_AVAILABLE("{$e}All map supports seem to be disabled.", 1, 2, 3),
    COMMAND_ADMIN_DYNMAP_RENDERED("{$p}Rendered a total of {$s}%lands% {$p}land sections.", 1, 2, 3),
    COMMAND_ADMIN_DYNMAP_REMOVED("{$p}A total of {$s}%lands% {$p}has been removed.", 1, 2, 3),

    @Path({"themes", "map-viewers", "fill", "displayname"})
    THEMES_MAP_VIEWERS_FILL_DISPLAYNAME("&2Map Viewers Fill Color"),
    @Path({"themes", "map-viewers", "fill-color", "description"})
    THEMES_MAP_VIEWERS_FILL_DESCRIPTION("The color used as the online map fill color for lands."),

    @Path({"themes", "map-viewers", "outline", "displayname"})
    THEMES_MAP_VIEWERS_OUTLINE_DISPLAYNAME("&2Map Viewers Outline Color"),
    @Path({"themes", "map-viewers", "outline", "description"})
    THEMES_MAP_VIEWERS_OUTLINE_DESCRIPTION("The color used as the online map outline color for land borders."),
    ;

    private final LanguageEntry languageEntry;
    private final String defaultValue;

    MapViewersLang(String defaultValue, int... group) {
        this.defaultValue = defaultValue;
        this.languageEntry = DefinedMessenger.getEntry("map-viewers", this, group);
    }

    @NonNull
    @Override
    public LanguageEntry getLanguageEntry() {
        return languageEntry;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }
}
