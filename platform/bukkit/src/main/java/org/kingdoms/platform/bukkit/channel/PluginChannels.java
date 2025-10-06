package org.kingdoms.platform.bukkit.channel;

import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftConnection;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftMapping;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftPackage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.server.location.BlockVector3;
import org.kingdoms.utils.internal.nonnull.NonNullMap;

import java.awt.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * https://wiki.vg/Plugin_channels
 *
 * minecraft:debug/game_test_add_marker -> Used to set different debug markers in the world.
 *                                         Never sent by the Notchian server.
 *                                         This interestingly renders without problems since 1.16.5.
 */
public final class PluginChannels {
    private static final Color INVISIBLE_MARKER = new Color(0, 0, 0, 0);
    private static final Duration REMOVE_MARKER_DURATION = Duration.ofSeconds(1);
    private static final int INVISIBLE_MARKER_ARGB = encodeARGB(INVISIBLE_MARKER);

    private static final MinecraftClassHandle ResourceLocation = XReflection.ofMinecraft()
            .inPackage(MinecraftPackage.NMS, "resources")
            .map(MinecraftMapping.MOJANG, "ResourceLocation")
            .map(MinecraftMapping.SPIGOT, "MinecraftKey")
            .map(MinecraftMapping.OBFUSCATED, "amj" /* 1.21.9 */);

    // Import with the same name because of papers remapper.
    private static final MethodHandle ResourceLocation_fromNamespaceAndPath;

    protected static final Map<UUID, Set<BlockMarkerPluginChannel>> MARKERS = NonNullMap.of(new ConcurrentHashMap<>());

    private static int encodeARGB(Color color) {
        return (0xFF & color.getAlpha()) << 24
                | (0xFF & color.getRed()) << 16
                | (0xFF & color.getGreen()) << 8
                | (0xFF & color.getBlue());
    }

    static {
        if (ResourceLocation.exists()) {
            ResourceLocation_fromNamespaceAndPath = ResourceLocation
                    .method("public static ResourceLocation fromNamespaceAndPath(String namespace, String key)")
                    .map(MinecraftMapping.OBFUSCATED, "a")
                    .reflectOrNull();
        } else {
            ResourceLocation_fromNamespaceAndPath = null;
        }
    }

    private static final Class<?> BlockPos = XReflection.ofMinecraft()
            .inPackage(MinecraftPackage.NMS, "core")
            .map(MinecraftMapping.MOJANG, "BlockPos")
            .map(MinecraftMapping.SPIGOT, "BlockPosition")
            .unreflect();
    private static final MethodHandle BlockPos$ctor = XReflection.of(BlockPos)
            .constructor(int.class, int.class, int.class)
            .unreflect();
    private static final MethodHandle GameTestAddMarkerDebugPayload$ctor = XReflection.ofMinecraft()
            .inPackage(MinecraftPackage.NMS, "network.protocol.common.custom")
            .named("GameTestAddMarkerDebugPayload")
            .constructor(BlockPos, int.class, String.class, int.class)
            .reflectOrNull();
    private static final MethodHandle ClientboundCustomPayloadPacket$ctor = XReflection.ofMinecraft()
            .inPackage(MinecraftPackage.NMS, "network.protocol.common")
            .named("ClientboundCustomPayloadPacket")
            .constructor(XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.protocol.common.custom").named("CustomPacketPayload"))
            .reflectOrNull();
    private static final MethodHandle ClientboundGameTestHighlightPosPacket = XReflection.ofMinecraft()
            .inPackage(MinecraftPackage.NMS, "network.protocol.game")
            .map(MinecraftMapping.MOJANG, "ClientboundGameTestHighlightPosPacket")
            .map(MinecraftMapping.SPIGOT, "ClientboundGameTestHighlightPosPacket")
            .map(MinecraftMapping.OBFUSCATED, "aed")
            .constructor(BlockPos, BlockPos)
            .reflectOrNull();

    public enum DefaultChannel {
        /**
         * https://wiki.vg/Plugin_channels#minecraft:debug.2Fgame_test_add_marker
         */
        DEBUG$GAME_TEST_ADD_MARKER,

        /**
         * There's currently no way to clear a specific marker.
         * https://wiki.vg/Plugin_channels#minecraft:debug/game_test_clear
         */
        DEBUG$GAME_TEST_CLEAR;

        private final Object resourceLocation;

        DefaultChannel() {
            try {
                this.resourceLocation = ResourceLocation_fromNamespaceAndPath.invoke("minecraft", this.name().replace('$', '/').toLowerCase(Locale.ENGLISH));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final MethodHandle SEND_CUSTOM_PAYLOAD = null;
    private static final boolean SUPPORTED =
            ResourceLocation_fromNamespaceAndPath != null && (GameTestAddMarkerDebugPayload$ctor != null || ClientboundGameTestHighlightPosPacket != null);
    // private static final MethodHandle SEND_CUSTOM_PAYLOAD = XReflection.namespaced()
    //         .imports("MinecraftKey", MinecraftKey.class)
    //         .ofMinecraft("package cb.entity; public class CraftPlayer {}")
    //         .method("private void sendCustomPayload(MinecraftKey id, byte[] message);")
    //         .reflectOrNull();


    public static boolean isSupported() {
        return SUPPORTED;
    }

    private static void ensureSupported() {
        if (!isSupported())
            throw new UnsupportedOperationException("Plugin channels are not supported in this version");
    }

    public static void clearBlockMarkers(@NotNull final Player player) {
        ensureSupported();
        sendPayload(player, DefaultChannel.DEBUG$GAME_TEST_CLEAR.resourceLocation, Unpooled.EMPTY_BUFFER);
    }

    public static void clearBlockMarkers(@NotNull final Player player, @NotNull Collection<BlockMarkerPluginChannel> markers) {
        if (ClientboundGameTestHighlightPosPacket != null) return; // No clear function.
        ensureSupported();

        Set<BlockMarkerPluginChannel> handles = PluginChannels.MARKERS.get(player.getUniqueId());

        // Yes! This trick actually works.
        // It seems like the markers are overridden entirely, so any duration will work.
        List<Object> packets = new ArrayList<>(markers.stream().mapToInt(x -> x.getMarkers().size()).sum());
        for (BlockMarkerPluginChannel marker : markers) {
            if (handles == null || !handles.isEmpty()) {
                if (!handles.remove(marker)) return;
            }

            for (Map.Entry<BlockVector3, BlockMarker> block : marker.getMarkers().entrySet()) {
                BlockVector3 loc = block.getKey();
                Object packet = createDebugMarkers(
                        loc.getX(), loc.getY(), loc.getZ(),
                        INVISIBLE_MARKER_ARGB, "", REMOVE_MARKER_DURATION
                );
                packets.add(packet);
            }
        }

        if (handles != null && handles.isEmpty()) PluginChannels.MARKERS.remove(player.getUniqueId());
        // TODO re-show other markers that are still active in the overlapping locations.
        MinecraftConnection.sendPacket(player, packets.toArray());
    }

    //
    // public static void sendBlockMarker(@NotNull final Player player, @NotNull BlockVector3 location, Color color, Duration duration) {
    //     Objects.requireNonNull(player, "player");
    //     Objects.requireNonNull(location, "location");
    //     Objects.requireNonNull(color, "color");
    //     Objects.requireNonNull(duration, "duration");
    //     ensureSupported();
    //
    //     ByteBuf packet = Unpooled.buffer();
    //     final int x = location.getX();
    //     final int y = location.getY();
    //     final int z = location.getZ();
    //
    //     // Location of the marker
    //     packet.writeLong(toBlockPositionBit(x, y, z));
    //
    //     // Encoded ARGB color
    //     int argb = (0xFF & color.getAlpha()) << 24
    //             | (0xFF & color.getRed()) << 16
    //             | (0xFF & color.getGreen()) << 8
    //             | (0xFF & color.getBlue());
    //     packet.writeInt(argb);
    //
    //     // Name of the marker
    //     // This will appear as a small version of holograms inside each block highlight, so it's not really useful.
    //     writeString(packet, "");
    //
    //     // Time in milliseconds, after which the marker will be destroyed
    //     packet.writeInt((int) duration.toMillis());
    //
    //     sendPayload(player, DefaultChannel.DEBUG$GAME_TEST_ADD_MARKER.getMinecraftKey(), packet);
    // }

    public static void sendBlockMarker(@NotNull final Player player, Collection<BlockVector3> blocks, BlockMarker marker) {
        Map<BlockVector3, BlockMarker> markers = new HashMap<>(blocks.size());

        for (BlockVector3 block : blocks) markers.put(block, marker);

        BlockMarkerPluginChannel channel = new BlockMarkerPluginChannel(markers);
        sendBlockMarker(player, Collections.singleton(channel));
    }

    public static void sendBlockMarker(@NotNull final Player player, Collection<BlockMarkerPluginChannel> markers) {
        ensureSupported();

        List<Object> packets = new ArrayList<>(markers.stream().mapToInt(x -> x.getMarkers().size()).sum());
        Set<BlockMarkerPluginChannel> handles = PluginChannels.MARKERS
                .computeIfAbsent(player.getUniqueId(), k -> Collections.newSetFromMap(new IdentityHashMap<>()));

        for (BlockMarkerPluginChannel marker : markers) {
            handles.add(marker);

            for (Map.Entry<BlockVector3, BlockMarker> block : marker.getMarkers().entrySet()) {
                BlockVector3 loc = block.getKey();
                BlockMarker props = block.getValue();

                Object packet = createDebugMarkers(
                        loc.getX(), loc.getY(), loc.getZ(),
                        encodeARGB(props.color), props.title, props.duration
                );
                packets.add(packet);
            }
        }

        MinecraftConnection.sendPacket(player, packets.toArray());
    }

    private static Object sendDebugMarker(int x, int y, int z, int argb, String title, Duration duration) {
        // BlockPos pos = new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        // long packedPos = BlockPos.asLong(pos.getX(), pos.getY(), pos.getZ());
        long packedPos = ((long) (x & 0x3FFFFFFL) << 38) |
                ((long) (z & 0x3FFFFFFL) << 12) |
                ((long) y & 0xFFF);

        try {
            // Serialize data to buffer (matches protocol order)
            Class<?> FriendlyByteBuf = XReflection.ofMinecraft()
                    .inPackage(MinecraftPackage.NMS, "network")
                    .map(MinecraftMapping.MOJANG, "FriendlyByteBuf")
                    .map(MinecraftMapping.SPIGOT, "PacketDataSerializer")
                    .map(MinecraftMapping.OBFUSCATED, "wn")
                    .unreflect();

            Object byteBuf = XReflection.of(FriendlyByteBuf)
                    .constructor(ByteBuf.class).unreflect()
                    .invoke(Unpooled.buffer());

            XReflection.of(FriendlyByteBuf)
                    .method("public FriendlyByteBuf writeLong(long arg0)").reflect()
                    .invoke(byteBuf, packedPos);

            XReflection.of(FriendlyByteBuf)
                    .method("public FriendlyByteBuf writeInt(int arg0)").reflect()
                    .invoke(byteBuf, argb);

            XReflection.of(FriendlyByteBuf)
                    .method("public int writeCharSequence(CharSequence arg0, java.nio.charset.Charset arg1)").reflect()
                    .invoke(byteBuf, title, StandardCharsets.UTF_8);

            XReflection.of(FriendlyByteBuf)
                    .method("public FriendlyByteBuf writeInt(int arg0)").reflect()
                    .invoke(byteBuf, (int) duration.toMillis());

            Class<?> DiscardedPayload = XReflection.ofMinecraft()
                    .inPackage(MinecraftPackage.NMS, "network.protocol.common.custom")
                    .map(MinecraftMapping.MOJANG, "DiscardedPayload")
                    .map(MinecraftMapping.SPIGOT, "DiscardedPayload")
                    .map(MinecraftMapping.OBFUSCATED, "abu")
                    .reflect();

            // This class is so fucking inconsistent. Some sources say it only hasa ResourceLocation parameter, some say
            // it has a second parameter ByteBuf and others say it's a byte[]...
            Object payload = XReflection.of(DiscardedPayload)
                    .constructor(ResourceLocation, XReflection.of(byte[].class)).reflect()
                    .invoke(DefaultChannel.DEBUG$GAME_TEST_ADD_MARKER.resourceLocation, ((ByteBuf) byteBuf).array());

            return ClientboundCustomPayloadPacket$ctor.invoke(payload);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Object createDebugMarkers(int x, int y, int z, int argb, String title, Duration duration) {
        // return sendDebugMarker(x, y, z, argb, title, duration);

        try {
            if (ClientboundGameTestHighlightPosPacket != null) {
                // We can't use colors :(
                return ClientboundGameTestHighlightPosPacket
                        .invoke(BlockPos$ctor.invoke(x, y, z), BlockPos$ctor.invoke(0, 0, 0));
            } else {
                Object blockPos = BlockPos$ctor.invoke(x, y, z);
                Object markerPayload = GameTestAddMarkerDebugPayload$ctor.invoke(blockPos, argb, title, (int) duration.toMillis());
                return ClientboundCustomPayloadPacket$ctor.invoke(markerPayload);
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private static long toBlockPositionBit(int x, int y, int z) {
        // https://wiki.vg/Protocol#Position
        return (((long) x & 0x3FFFFFF) << 38)
                | ((long) y & 0xFFF)
                | (((long) z & 0x3FFFFFF) << 12);
    }

    private static void wrap(ByteBuf packet, int i) {
        while ((i & -128) != 0) {
            packet.writeByte(i & 127 | 128);
            i >>>= 7;
        }
        packet.writeByte(i);
    }

    private static void writeString(@NotNull final ByteBuf packet, @NotNull final String string) {
        byte[] byteArray = string.getBytes(StandardCharsets.UTF_8);
        wrap(packet, byteArray.length);
        packet.writeBytes(byteArray);
    }

    private static void sendPayload(@NotNull final Player receiver, Object resourceLocation, ByteBuf bytes) {
        Objects.requireNonNull(receiver, "receiver");
        Objects.requireNonNull(resourceLocation, "resourceLocation");
        Objects.requireNonNull(bytes, "bytes");
        ensureSupported();

        // 1.19
        // PacketPlayOutCustomPayload customPayload = new PacketPlayOutCustomPayload(
        //         new MinecraftKey(channel),
        //         new PacketDataSerializer(bytes)
        // );
        // 1.20
        // ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(new CustomPacketPayload() {
        //     public void a(PacketDataSerializer pds) {
        //         pds.writeBytes(bytes);
        //     }
        //
        //     public MinecraftKey a() {
        //         return new MinecraftKey("minecraft", channel);
        //     }
        // });
        // MinecraftConnection.sendPacket(receiver, packet);
        try {
            // https://stackoverflow.com/questions/19296386/netty-java-getting-data-from-bytebuf
            // Dont use bytes.array()
            byte[] arr = new byte[bytes.readableBytes()];
            bytes.readBytes(arr);

            SEND_CUSTOM_PAYLOAD.invoke(receiver, resourceLocation, arr);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}