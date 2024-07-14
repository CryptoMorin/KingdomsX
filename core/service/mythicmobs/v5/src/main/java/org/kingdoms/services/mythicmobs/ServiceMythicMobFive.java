package org.kingdoms.services.mythicmobs;

import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.adapters.AbstractWorld;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.kingdoms.services.ServiceMythicMobs;
import org.kingdoms.services.UnknownMythicMobException;

import java.util.Optional;

public final class ServiceMythicMobFive implements ServiceMythicMobs {
    @Override
    public Entity spawnMythicMob(Location location, String mob, int level) {
        //.getAPIHelper().spawnMythicMob(mob, location, level);
        Optional<MythicMob> mm = MythicProvider.get().getMobManager().getMythicMob(mob);
        if (!mm.isPresent()) throw new UnknownMythicMobException(mob);

        ActiveMob activeMob = mm.get().spawn(toLumineLocation(location), level);
        return activeMob.getEntity().getBukkitEntity();
    }

    @Override
    public Throwable checkAvailability() {
        try {
            MythicProvider.get().getMobManager();
            return null;
        } catch (Throwable ex) {
            return ex;
        }
    }

    static AbstractLocation toLumineLocation(Location loc) {
        @SuppressWarnings("resource") AbstractWorld world = MythicBukkit.inst().getBootstrap().getWorld(loc.getWorld().getUID());
        return new AbstractLocation(world, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }
}
