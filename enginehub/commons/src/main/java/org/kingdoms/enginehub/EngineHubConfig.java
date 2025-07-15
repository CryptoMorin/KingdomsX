package org.kingdoms.enginehub;

import org.bukkit.plugin.Plugin;
import org.kingdoms.config.accessor.EnumConfig;
import org.kingdoms.config.accessor.KeyedConfigAccessor;
import org.kingdoms.config.implementation.KeyedYamlConfigAccessor;
import org.kingdoms.config.managers.ConfigManager;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.utils.config.ConfigPath;
import org.kingdoms.utils.config.adapters.YamlResource;
import org.kingdoms.utils.string.Strings;

public enum EngineHubConfig implements EnumConfig {
    WORLDEDIT_SCHEMATICS_ENABLED(1, 2),
    WORLDEDIT_SCHEMATICS_FORCE_EXTENSION(1, 2),
    WORLDEDIT_SCHEMATICS_DEFAULT_SAVE_FORMAT(1, 2),
    WORLDEDIT_SCHEMATICS_LOADING_MECHANISM_FORCED_FORMAT(1, 2, 4),
    WORLDEDIT_SCHEMATICS_LOADING_MECHANISM_BYPASS_FAWE_EXTENSION_CHECK(1, 2, 4),
    WORLDEDIT_EDIT_PROTECTION(1),

    WORLDGUARD_REGISTER_FLAGS(1),
    WORLDGUARD_REGION_CLAIM_PROTECTION(1),
    WORLDGUARD_PROTECTED_REGION_RADIUS(1),
    WORLDGUARD_INDICATOR_IGNORE_REGIONS(1),
    WORLDGUARD_MAP_MARKERS(1),
    WORLDGUARD_KEEP_KINGDOMS_FLIGHT_WITH_FLAG(1),
    ;

    private static final String FILE_NAME = "enginehub";
    public static YamlResource ENGINE_HUB;

    public static void register(Plugin plugin) {
        ENGINE_HUB = new YamlResource(plugin,
                Kingdoms.getPath(FILE_NAME + ".yml").toFile(), FILE_NAME + ".yml").load();

        ConfigManager.registerAsMainConfig(ENGINE_HUB);
        ConfigManager.watch(ENGINE_HUB);
    }

    private final ConfigPath option;

    @SuppressWarnings("unused")
    EngineHubConfig() {
        this.option = new ConfigPath(Strings.configOption(this));
    }

    EngineHubConfig(int... grouped) {
        this.option = new ConfigPath(this.name(), grouped);
    }

    @Override
    public KeyedConfigAccessor getManager() {
        return new KeyedYamlConfigAccessor(ENGINE_HUB, option);
    }

    public static YamlResource getConfig() {
        return ENGINE_HUB;
    }
}
