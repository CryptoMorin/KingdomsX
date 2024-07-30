package org.kingdoms.server.platform;

import org.kingdoms.utils.internal.reflection.Reflect;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class CrossPlatformNotifier {
    /**
     * Tested with Magma, Mohist, and Geyser
     */
    protected static boolean isRunningForge() {
        // https://github.com/MinecraftForge/MinecraftForge/blob/1.19.x/fmlloader/src/main/java/net/minecraftforge/fml/server/ServerMain.java
        String fml = "net.minecraftforge.fml.";
        return Stream.of("common.Mod", "common.Loader", "common.FMLContainer", "ModLoader", "client.FMLClientHandler", "server.ServerMain")
                .anyMatch(x -> Reflect.classExists(fml + x));
    }

    protected static boolean isRunningGeyser() {
        // https://github.com/GeyserMC/Geyser
        String geyser = "org.geysermc.";
        return Stream.of("geyser.GeyserMain", "geyser.Constants", "connector.GeyserConnector",
                        "connector.network.session.GeyserSession", "api.Geyser", "api.connection.Connection")
                .anyMatch(x -> Reflect.classExists(geyser + x));
    }

    protected static boolean isRunningFloodGate() {
        return Reflect.classExists("org.geysermc.floodgate.api.FloodgateApi");
    }

    protected static boolean isRunningBukkit() {
        return Reflect.classExists("org.bukkit.entity.Player") && Reflect.classExists("org.bukkit.Bukkit");
    }

    protected static boolean isRunningPaper() {
        return Reflect.classExists("com.destroystokyo.paper.PaperConfig") || Reflect.classExists("io.papermc.paper.configuration.Configuration");
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    public static boolean isRunningFolia() {
        // https://github.com/PaperMC/Folia/blob/master/patches/api/0004-Add-RegionizedServerInitEvent.patch
        if (Reflect.classExists("io.papermc.paper.threadedregions.RegionizedServerInitEvent")) {
            try {
                // https://github.com/PaperMC/Folia/blob/master/patches/api/0003-Require-plugins-to-be-explicitly-marked-as-Folia-sup.patch
                Class.forName("org.bukkit.plugin.PluginDescriptionFile").getDeclaredMethod("isFoliaSupported");
                return true;
            } catch (Throwable ex) {
                return false;
            }
        }

        return false;
    }

    public static boolean isRunningSpigot() {
        return Reflect.classExists("org.spigotmc.SpigotConfig");
    }

    public static List<String> warn() {
        List<String> warnings = new ArrayList<>();
        if (Platform.FORGE.isAvailable()) {
            warnings.add("Your server is running on a platform that supports Forge. The plugin may not function properly.");
        }
        if (Platform.BEDROCK.isAvailable()) {
            warnings.add("Your server is running on a platform that supports Bedrock Edition. The plugin may not function properly.");
        }
        if (Platform.FOLIA.isAvailable()) {
            warnings.add("Your server is running on Folia. The plugin has not added support for this software, and the plugin will most likely not work.");
        }

        return warnings;
    }
}
