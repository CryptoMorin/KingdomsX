package org.kingdoms.platform.bukkit.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.kingdoms.platform.folia.PluginTickTracker;

public class BukkitTickTracker implements PluginTickTracker {
    private int ticks;

    @Override
    public void start(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> ticks++, 0L, 1L);
    }

    @Override
    public int getTicks() {
        return ticks;
    }
}
