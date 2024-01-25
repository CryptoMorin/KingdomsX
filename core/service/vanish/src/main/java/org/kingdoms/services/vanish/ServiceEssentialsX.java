package org.kingdoms.services.vanish;

import com.earth2me.essentials.Essentials;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

// https://github.com/EssentialsX/Essentials/blob/2.x/Essentials/src/main/java/com/earth2me/essentials/User.java
public final class ServiceEssentialsX implements ServiceCommons {
    private static final Essentials ESS = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");

    @Override
    public boolean isVanished(Player player) {
        return ESS.getUser(player).isVanished();
    }

    @Override
    public boolean isInGodMode(Player player) {
        return ESS.getUser(player).isGodModeEnabled();
    }

    @Override
    public boolean isIgnoring(Player ignorant, Player ignoring) {
        return ESS.getUser(ignorant).isIgnoredPlayer(ESS.getUser(ignoring));
    }
}
