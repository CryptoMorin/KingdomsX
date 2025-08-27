package org.kingdoms.platform.folia;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;

public final class FoliaUtil {
    private FoliaUtil() {}

    public static boolean isFoliaSupported() {
        // https://docs.papermc.io/paper/dev/folia-support/#checking-for-folia
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static CompletableFuture<Boolean> teleport(Entity entity, Entity location) {
        return entity.teleportAsync(location.getLocation());
    }

    public static CompletableFuture<Boolean> teleport(Entity entity, Location location) {
        return entity.teleportAsync(location);
    }
}
