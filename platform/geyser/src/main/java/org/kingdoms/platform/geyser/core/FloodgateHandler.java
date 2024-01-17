package org.kingdoms.platform.geyser.core;

import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public final class FloodgateHandler {
    public static boolean isFloodgatePlayer(UUID player) {
        return FloodgateApi.getInstance().isFloodgatePlayer(player);
    }
}
