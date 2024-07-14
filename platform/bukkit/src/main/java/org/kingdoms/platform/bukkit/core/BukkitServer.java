package org.kingdoms.platform.bukkit.core;

import org.bukkit.plugin.java.JavaPlugin;
import org.kingdoms.platform.bukkit.adapters.BukkitNBTAdapter;
import org.kingdoms.platform.bukkit.events.BukkitEventHandler;
import org.kingdoms.platform.bukkit.location.BukkitWorldRegistry;
import org.kingdoms.server.core.Server;
import org.kingdoms.server.events.EventHandler;
import org.kingdoms.server.location.WorldRegistry;

public class BukkitServer implements Server {
    private final EventHandler eventHandler;
    private final WorldRegistry worldRegistry;
    private final JavaPlugin plugin;
    private final TickTracker tickTracker;

    public BukkitServer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.worldRegistry = new BukkitWorldRegistry();
        this.tickTracker = new TickTracker();
        this.eventHandler = new BukkitEventHandler(this);
    }

    @Override
    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    @Override
    public void onStartup() {
        BukkitNBTAdapter.registerAll();
    }

    @Override
    public void onEnable() {
        tickTracker.start(plugin);
        this.eventHandler.onLoad();
    }

    @Override
    public int getTicks() {
        return tickTracker.getTicks();
    }

    @Override
    public WorldRegistry getWorldRegistry() {
        return worldRegistry;
    }
}
