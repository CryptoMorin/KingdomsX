package org.kingdoms.platform.folia;

import org.bukkit.plugin.Plugin;

public final class FoliaTickTracker implements PluginTickTracker {
    private int ticks;

    @Override
    public void start(Plugin plugin) {
        FoliaSchedulerFactory.GLOBAL_SCHEDULER.runAtFixedRate(plugin, task -> ticks++, 1L, 1L);
    }

    @Override
    public int getTicks() {
        return ticks;
    }
}
