package org.kingdoms.services.vanish;

import com.earth2me.essentials.Essentials;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ServiceEssentialsX implements ServiceVanish {
    private static final Essentials ESS = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");

    @Override
    public boolean isVanished(Player player) {
        return ESS.getUser(player).isVanished();
    }

    @Override
    public boolean isInGodMode(Player player) {
        return ESS.getUser(player).isGodModeEnabled();
    }
}
