package org.kingdoms.enginehub.worldguard.handlers;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.enginehub.worldguard.ServiceWorldGuard;
import org.kingdoms.managers.pvp.PvPManager;

public final class WorldGuradPvPHandler implements PvPManager.PvPHandler {
    private final ServiceWorldGuard worldGuard;

    public WorldGuradPvPHandler(ServiceWorldGuard worldGuard) {this.worldGuard = worldGuard;}

    @Override
    public Boolean canFight(@NonNull Player victim, @NonNull Player damager) {
        if (worldGuard.hasFriendlyFireFlag(damager) && worldGuard.hasFriendlyFireFlag(victim)) return true;
        return null;
    }
}
