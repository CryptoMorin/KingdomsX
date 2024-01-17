package org.kingdoms.platform.bukkit.events;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.kingdoms.server.events.EventHandler;

public final class BukkitEventHandler implements EventHandler {
    @Override
    public void callEvent(Object event) {
        Bukkit.getPluginManager().callEvent((Event) event);
    }
}
