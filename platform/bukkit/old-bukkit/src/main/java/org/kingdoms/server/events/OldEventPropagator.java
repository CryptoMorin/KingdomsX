package org.kingdoms.server.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.kingdoms.server.core.Server;
import org.kingdoms.utils.internal.reflection.Reflect;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;

public final class OldEventPropagator {
    private OldEventPropagator() {}

    public static final class Dismount implements Listener, EventPropagator {
        @EventHandler
        public void onEntityDismount(EntityDismountEvent event) {
            org.kingdoms.server.events.EntityDismountEvent delegate = new org.kingdoms.server.events.EntityDismountEvent(event.getEntity(), event.getDismounted());
            Server.get().getEventHandler().callEvent(delegate);
        }

        @EventHandler
        public void onEntityDismount(EntityMountEvent event) {
            org.kingdoms.server.events.EntityMountEvent delegate = new org.kingdoms.server.events.EntityMountEvent(event.getEntity(), event.getMount());
            Server.get().getEventHandler().callEvent(delegate);
        }

        @Override
        public boolean shouldRegister() {
            return !Reflect.classExists("org.bukkit.event.entity.EntityDismountEvent");
        }
    }
}
