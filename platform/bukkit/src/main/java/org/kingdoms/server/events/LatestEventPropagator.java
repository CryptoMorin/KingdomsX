package org.kingdoms.server.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.kingdoms.server.core.Server;
import org.kingdoms.utils.internal.reflection.Reflect;

public final class LatestEventPropagator {
    public static final class Dismount implements Listener, EventPropagator {
        @EventHandler
        public void onEntityDismount(org.bukkit.event.entity.EntityDismountEvent event) {
            EntityDismountEvent delegate = new EntityDismountEvent(event.getEntity(), event.getDismounted());
            delegate.setCancelled(event.isCancelled());
            Server.get().getEventHandler().callEvent(delegate);
        }

        @EventHandler
        public void onEntityDismount(EntityMountEvent event) {
            org.kingdoms.server.events.EntityMountEvent delegate = new org.kingdoms.server.events.EntityMountEvent(event.getEntity(), event.getMount());
            Server.get().getEventHandler().callEvent(delegate);
        }

        @Override
        public boolean shouldRegister() {
            return Reflect.classExists("org.bukkit.event.entity.EntityDismountEvent");
        }
    }
}
