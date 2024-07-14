package org.kingdoms.utils.internal.jdk;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.GameTestAddMarkerDebugPayload;

import java.time.Duration;

public final class RecordAccessor {
    public static Object createCustomPayload(int x, int y, int z, int argb, Duration duration) {
        return new ClientboundCustomPayloadPacket(new GameTestAddMarkerDebugPayload(
                new BlockPosition(x, y, z), argb, "", (int) duration.toMillis()
        ));
    }
}
