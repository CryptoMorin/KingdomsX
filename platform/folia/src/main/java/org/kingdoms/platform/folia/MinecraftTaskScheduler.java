package org.kingdoms.platform.folia;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.kingdoms.scheduler.TaskScheduler;

public interface MinecraftTaskScheduler {
    TaskScheduler of(Entity entity);

    TaskScheduler of(Location location);

    TaskScheduler of(World world, int chunkX, int chunkZ);
}
