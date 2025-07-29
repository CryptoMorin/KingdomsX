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
        if (isFoliaSupported()) {
            return entity.teleportAsync(location.getLocation());
        } else {
            return CompletableFuture.completedFuture(entity.teleport(location));
        }
    }

    public static CompletableFuture<Boolean> teleport(Entity entity, Location location) {
        if (isFoliaSupported()) {
            return entity.teleportAsync(location);
        } else {
            return CompletableFuture.completedFuture(entity.teleport(location));
        }
    }
}
