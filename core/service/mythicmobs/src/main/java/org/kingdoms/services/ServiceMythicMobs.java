package org.kingdoms.services;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface ServiceMythicMobs extends Service {
    Entity spawnMythicMob(Location location, String name, int level);
}
