package org.kingdoms.platform.bukkit.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.kingdoms.platform.bukkit.adapters.BukkitNBTAdapter;
import org.kingdoms.platform.bukkit.events.BukkitEventHandler;
import org.kingdoms.platform.bukkit.location.BukkitWorldRegistry;
import org.kingdoms.platform.folia.FoliaSchedulerFactory;
import org.kingdoms.platform.folia.FoliaTickTracker;
import org.kingdoms.platform.folia.FoliaUtil;
import org.kingdoms.platform.folia.PluginTickTracker;
import org.kingdoms.server.core.Server;
import org.kingdoms.server.events.EventHandler;
import org.kingdoms.server.location.WorldRegistry;

import java.util.ArrayList;
import java.util.List;

public class BukkitServer implements Server {
    private final EventHandler eventHandler;
    private final WorldRegistry worldRegistry;
    private final JavaPlugin plugin;
    private final PluginTickTracker tickTracker;
    private boolean isReady;
    private final long startTime = System.currentTimeMillis();
    private final List<Runnable> onReadyRunnables = new ArrayList<>();

    @SuppressWarnings("this-escape")
    public BukkitServer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.worldRegistry = new BukkitWorldRegistry();
        this.tickTracker = FoliaUtil.isFoliaSupported() ? new FoliaTickTracker() : new BukkitTickTracker();
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

        if (FoliaUtil.isFoliaSupported()) {
            FoliaSchedulerFactory.runGlobalUntraced(plugin, this::onReady);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> onReady());
        }
    }

    @Override
    public void onReady(Runnable action) {
        if (isReady) {
            action.run();
        } else {
            onReadyRunnables.add(action);
        }
    }

    @Override
    public void onReady() {
        if (isReady) throw new IllegalStateException("Server was already ready");
        this.isReady = true;
        onReadyRunnables.forEach(Runnable::run);
        onReadyRunnables.clear();
    }

    @Override
    public boolean isReady() {
        return isReady;
    }

    @Override
    public int getTicks() {
        return tickTracker.getTicks();
    }

    @Override
    public long startTime() {
        return startTime;
    }

    @Override
    public boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public WorldRegistry getWorldRegistry() {
        return worldRegistry;
    }
}
