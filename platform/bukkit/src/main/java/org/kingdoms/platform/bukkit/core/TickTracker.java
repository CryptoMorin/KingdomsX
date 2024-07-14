package org.kingdoms.platform.bukkit.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class TickTracker {
    // Using int, it can track up to:
    // (((2147483647 / 20) / 60) / 60) / 24
    // 1243 days
    private int ticks;

    public void start(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> ticks++, 0L, 1L);
    }

    public int getTicks() {
        return ticks;
    }
}
