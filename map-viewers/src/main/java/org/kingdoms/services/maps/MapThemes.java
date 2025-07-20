package org.kingdoms.services.maps;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.constants.themes.AbstractTheme;
import org.kingdoms.constants.themes.ThemeProvider;
import org.kingdoms.constants.themes.Themes;
import org.kingdoms.locale.messenger.Messenger;

public final class MapThemes {
    private static final Namespace FILL_COLOR = new Namespace("MapViewers", "FILL");
    private static final Namespace OUTLINE_COLOR = new Namespace("MapViewers", "OUTLINE");

    public static final ThemeProvider FILL = new Fill();
    public static final ThemeProvider OUTLINE = new Outline();

    private MapThemes() {}

    protected static void register() {
        Themes.INSTANCE.register(FILL);
        Themes.INSTANCE.register(OUTLINE);
    }

    private static final class Fill extends AbstractTheme {
        @NotNull
        @Override
        public Messenger getDisplayName() {
            return MapViewersLang.THEMES_MAP_VIEWERS_FILL_DISPLAYNAME;
        }

        @NotNull
        @Override
        public Messenger getDescription() {
            return MapViewersLang.THEMES_MAP_VIEWERS_FILL_DESCRIPTION;
        }

        @Override
        public @NonNull Namespace getNamespace() {
            return FILL_COLOR;
        }
    }

    private static final class Outline extends AbstractTheme {
        @NotNull
        @Override
        public Messenger getDisplayName() {
            return MapViewersLang.THEMES_MAP_VIEWERS_OUTLINE_DISPLAYNAME;
        }

        @NotNull
        @Override
        public Messenger getDescription() {
            return MapViewersLang.THEMES_MAP_VIEWERS_OUTLINE_DESCRIPTION;
        }

        @Override
        public @NonNull Namespace getNamespace() {
            return OUTLINE_COLOR;
        }
    }
}
