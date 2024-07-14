package org.kingdoms.services.mythicmobs;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.api.exceptions.InvalidMobTypeException;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.kingdoms.services.ServiceMythicMobs;
import org.kingdoms.services.UnknownMythicMobException;

public final class ServiceMythicMobFour implements ServiceMythicMobs {
    @Override
    public Entity spawnMythicMob(Location location, String mob, int level) {
        try {
            return MythicMobs.inst().getAPIHelper().spawnMythicMob(mob, location, level);
        } catch (InvalidMobTypeException e) {
            throw new UnknownMythicMobException(mob, e);
        }
    }

    @Override
    public Throwable checkAvailability() {
        try {
            MythicMobs.inst().getAPIHelper();
            return null;
        } catch (Throwable ex) {
            return ex;
        }
    }
}
