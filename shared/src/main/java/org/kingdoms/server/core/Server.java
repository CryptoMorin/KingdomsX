package org.kingdoms.server.core;

import org.kingdoms.server.events.EventHandler;
import org.kingdoms.server.location.WorldRegistry;

import java.util.Objects;

public interface Server {
    static void init(Server instance) {
        ServerContainer.INSTANCE = instance;
    }

    static Server get() {
        return Objects.requireNonNull(ServerContainer.INSTANCE, "Server instance not initiated yet");
    }

    EventHandler getEventHandler();

    int getTicks();

    WorldRegistry getWorldRegistry();

    default void onStartup() {}

    default void onReady() {}

    default void onEnable() {}
}
