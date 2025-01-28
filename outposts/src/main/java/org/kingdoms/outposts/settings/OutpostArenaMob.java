package org.kingdoms.outposts.settings;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.locale.KingdomsLang;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.locale.provider.CascadingMessageContextProvider;
import org.kingdoms.main.KLogger;
import org.kingdoms.utils.compilers.expressions.MathExpression;
import org.kingdoms.utils.config.ConfigSection;

import java.time.Duration;

public final class OutpostArenaMob implements CascadingMessageContextProvider {
    private String label;
    private ConfigSection entitySettings;
    private int maxSpawnCount;
    private Duration spawnInterval;
    private Location spawnLocation;
    private MathExpression damageBonus;

    public OutpostArenaMob(String label, ConfigSection entitySettings, int maxSpawnCount, Duration spawnInterval, Location spawnLocation, MathExpression damageBonus) {
        this.label = label;
        this.entitySettings = entitySettings;
        this.maxSpawnCount = maxSpawnCount;
        this.spawnInterval = spawnInterval;
        this.spawnLocation = spawnLocation;
        this.damageBonus = damageBonus;
    }

    public ConfigSection getEntitySettings() {
        return entitySettings;
    }

    public int getMaxSpawnCount() {
        return maxSpawnCount;
    }

    public Duration getSpawnInterval() {
        return spawnInterval;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public String getLabel() {
        return label;
    }

    public MathExpression getDamageBonus() {
        return damageBonus;
    }

    public void setEntitySettings(ConfigSection entitySettings) {
        this.entitySettings = entitySettings;
    }

    public void setMaxSpawnCount(int maxSpawnCount) {
        this.maxSpawnCount = maxSpawnCount;
    }

    public void setSpawnInterval(Duration spawnInterval) {
        this.spawnInterval = spawnInterval;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setDamageBonus(MathExpression damageBonus) {
        this.damageBonus = damageBonus;
    }

    @Override
    public void addMessageContextEdits(@NotNull MessagePlaceholderProvider context) {
        context.addChild("arenamob", new MessagePlaceholderProvider()
                .parse("label", this.label == null ? KingdomsLang.UNKNOWN : label)
                .raw("spawn_interval", spawnInterval == null ? KingdomsLang.NONE : spawnInterval)
                .raw("max_spawn_count", maxSpawnCount)
                .raw("spawn_location", spawnLocation)
                .raw("damage_bonus", damageBonus == null ? KingdomsLang.NONE : damageBonus.getOriginalString())
        );
    }
}
