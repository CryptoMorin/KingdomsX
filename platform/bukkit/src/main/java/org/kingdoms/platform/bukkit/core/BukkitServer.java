package org.kingdoms.platform.bukkit.core;

import org.kingdoms.platform.bukkit.events.BukkitEventHandler;
import org.kingdoms.server.core.Server;
import org.kingdoms.server.events.EventHandler;

public class BukkitServer implements Server {
    private final EventHandler eventHandler;

    public BukkitServer() {
        this.eventHandler = new BukkitEventHandler();
    }

    @Override
    public EventHandler getEventHandler() {
        return eventHandler;
    }
}
