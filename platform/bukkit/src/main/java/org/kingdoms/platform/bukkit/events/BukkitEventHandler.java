package org.kingdoms.platform.bukkit.events;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.platform.bukkit.core.BukkitServer;
import org.kingdoms.server.events.EventHandler;
import org.kingdoms.server.events.EventPropagator;
import org.kingdoms.server.events.LatestEventPropagator;
import org.kingdoms.server.events.OldEventPropagator;

import java.util.stream.Stream;

public final class BukkitEventHandler implements EventHandler {
    private final BukkitServer server;

    public BukkitEventHandler(BukkitServer server) {
        this.server = server;
    }

    public void onLoad() {
        Stream.of(new OldEventPropagator.Dismount(), new LatestEventPropagator.Dismount())
                .filter(EventPropagator::shouldRegister).forEach(this::registerEvents);
    }

    @Override
    public void callEvent(Object event) {
        Bukkit.getPluginManager().callEvent((Event) event);
    }

    @Override
    public void registerEvents(@NotNull Object container) {
        Bukkit.getPluginManager().registerEvents((Listener) container, server.getPlugin());
    }
}
