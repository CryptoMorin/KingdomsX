package org.kingdoms.peacetreaties.config;

import org.kingdoms.config.accessor.EnumConfig;
import org.kingdoms.config.accessor.KeyedConfigAccessor;
import org.kingdoms.config.implementation.KeyedYamlConfigAccessor;
import org.kingdoms.config.managers.ConfigManager;
import org.kingdoms.config.managers.ConfigWatcher;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.peacetreaties.PeaceTreatiesAddon;
import org.kingdoms.peacetreaties.terms.TermRegistry;
import org.kingdoms.utils.config.ConfigPath;
import org.kingdoms.utils.config.adapters.YamlResource;
import org.kingdoms.utils.string.Strings;

public enum PeaceTreatyConfig implements EnumConfig {
    DURATION,
    MIN_TERMS,
    FORCE_ACCEPT_CONDITION(2),
    FORCE_ACCEPT_WAR_POINTS(2),
    UNFINISHED_CONTRACT_REMINDER,
    WAR_POINTS_MAX(2),
    WAR_POINTS_ALLOWED_RELATIONSHIPS(2),
    WAR_POINTS_SCORES_GAIN_KILL(2, 3, 4),
    WAR_POINTS_SCORES_LOSE_KILL(2, 3, 4),
    WAR_POINTS_SCORES_GAIN_BREAK_STRUCTURE(2, 3, 4),
    WAR_POINTS_SCORES_LOSE_BREAK_STRUCTURE(2, 3, 4),
    WAR_POINTS_SCORES_GAIN_BREAK_TURRET(2, 3, 4),
    WAR_POINTS_SCORES_LOSE_BREAK_TURRET(2, 3, 4),
    WAR_POINTS_SCORES_GAIN_INVADE(2, 3, 4),
    WAR_POINTS_SCORES_LOSE_INVADE(2, 3, 4),
    ;

    public static final YamlResource PEACE_TREATIES =
            new YamlResource(PeaceTreatiesAddon.get(),
                    Kingdoms.getPath("peace-treaties.yml").toFile(), "peace-treaties.yml").load();

    static {
        ConfigWatcher.register(PEACE_TREATIES.getFile().toPath().getParent(), ConfigWatcher::handleNormalConfigs);
        ConfigManager.registerNormalWatcher("peace-treaties", (event) -> {
            ConfigWatcher.reload(PEACE_TREATIES, "peace-treaties.yml");
            TermRegistry.loadTermGroupings();
        });
    }

    private final ConfigPath option;

    PeaceTreatyConfig() {
        this.option = new ConfigPath(Strings.configOption(this));
    }

    PeaceTreatyConfig(int... grouped) {
        this.option = new ConfigPath(this.name(), grouped);
    }

    @Override
    public KeyedConfigAccessor getManager() {
        return new KeyedYamlConfigAccessor(PEACE_TREATIES, option);
    }

    public static YamlResource getConfig() {
        return PEACE_TREATIES;
    }
}
