package org.kingdoms.utils.internal.jdk;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.game.ClientboundDebugEventPacket;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.debug.DebugStructureInfo;
import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import org.bukkit.craftbukkit.v1_21_R6.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public final class RecordAccessor {
    public static Object createCustomPayload(int x, int y, int z, int argb, String title, Duration duration) {
        // 1.21.9+
        // System.out.println("sending block pos " + x + " - " + y + " - " + z);

        int x2 = x + 10;
        int y2 = y + 10;
        int z2 = z + 10;
        try {
            BlockPosition min = new BlockPosition(Math.min(x, x2), Math.min(y, y2), Math.min(z, z2));
            BlockPosition max = new BlockPosition(Math.max(x, x2), Math.max(y, y2), Math.max(z, z2));

            // Create a single piece for the bounding box (can add more for complex structures)
            StructureBoundingBox bounding = StructureBoundingBox.a(min, max);
            DebugStructureInfo.a piece = new DebugStructureInfo.a(bounding, true); // true for start piece
            DebugStructureInfo structureInfo = new DebugStructureInfo(bounding, List.of(piece));

            ClientboundDebugEventPacket packet = new ClientboundDebugEventPacket(new DebugSubscription.a<>(DebugSubscriptions.m, List.of(structureInfo)));
            System.out.println("Sending debug packet " + packet);
            return packet;
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public static final class GameTestAddMarkerPayload implements CustomPacketPayload {
        private final long packedPos;
        private final int colorArgb;
        private final String text;
        private final int lifetimeMs;

        public GameTestAddMarkerPayload(long packedPos, int colorArgb, String text, int lifetimeMs) {
            this.packedPos = packedPos;
            this.colorArgb = colorArgb;
            this.text = text;
            this.lifetimeMs = lifetimeMs;
        }

        public int getLifetimeMs() {
            return lifetimeMs;
        }

        public int getColorArgb() {
            return colorArgb;
        }

        public long getPackedPos() {
            return packedPos;
        }

        public String getText() {
            return text;
        }

        //            ByteBufCodecs.LONG, GameTestAddMarkerPayload::getPackedPos,
        //                 ByteBufCodecs.INT, GameTestAddMarkerPayload::getColorArgb,
        //                 ByteBufCodecs.STRING, GameTestAddMarkerPayload::getText,
        //                 ByteBufCodecs.INT, GameTestAddMarkerPayload::getLifetimeMs,
        public static final StreamCodec<RegistryFriendlyByteBuf, GameTestAddMarkerPayload> CODEC = StreamCodec.a(
                ByteBufCodecs.j, GameTestAddMarkerPayload::getPackedPos,
                ByteBufCodecs.g, GameTestAddMarkerPayload::getColorArgb,
                ByteBufCodecs.p, GameTestAddMarkerPayload::getText,
                ByteBufCodecs.g, GameTestAddMarkerPayload::getLifetimeMs,
                GameTestAddMarkerPayload::new
        );

        // Type instance (binds ID + codec; used for packet construction)
        public static final CustomPacketPayload.c<RegistryFriendlyByteBuf, GameTestAddMarkerPayload> TYPE =
                new CustomPacketPayload.c<>(new CustomPacketPayload.b<>(MinecraftKey.a("minecraft", "debug/game_test_add_marker")), CODEC);

        @Override
        public b<GameTestAddMarkerPayload> a() {
            return TYPE.a();
        }
    }

    public static Object sendDebugMarkerClass(int x, int y, int z, int argb, String title, Duration duration) {
        long packedPos = ((long) (x & 0x3FFFFFFL) << 38) |
                ((long) (z & 0x3FFFFFFL) << 12) |
                ((long) y & 0xFFF);

        // Create the packet with the channel
        GameTestAddMarkerPayload payload = new GameTestAddMarkerPayload(packedPos, argb, title, (int) duration.toMillis());
        return new ClientboundCustomPayloadPacket(payload);
    }

    // public static Object sendDebugMarker(int x, int y, int z, int argb, String title, Duration duration) {
    //     // BlockPos pos = new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    //     // long packedPos = BlockPos.asLong(pos.getX(), pos.getY(), pos.getZ());
    //     long packedPos = ((long) (x & 0x3FFFFFFL) << 38) |
    //             ((long) (z & 0x3FFFFFFL) << 12) |
    //             ((long) y & 0xFFF);
    //
    //     // Serialize data to buffer (matches protocol order)
    //     PacketDataSerializer buffer = new PacketDataSerializer(Unpooled.buffer());
    //     buffer.writeLong(packedPos);         // Packed BlockPos
    //     buffer.writeInt(argb);          // ARGB color (e.g., 0xFFFF0000 for red)
    //     buffer.writeCharSequence(title, StandardCharsets.UTF_8);       // Label text (capped at 32,767 bytes)
    //     buffer.writeInt((int duration.toMillis());         // Lifetime in ms
    //
    //     DiscardedPayload payload = new DiscardedPayload(MinecraftKey.a("minecraft", "debug/game_test_add_marker"), buffer);
    //     return new ClientboundCustomPayloadPacket(payload);
    // }
}
