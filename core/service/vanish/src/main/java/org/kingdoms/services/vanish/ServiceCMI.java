package org.kingdoms.services.vanish;

import com.Zrips.CMI.CMI;
import org.bukkit.entity.Player;

public final class ServiceCMI implements ServiceVanish {
    @Override
    public boolean isVanished(Player player) {
        return CMI.getInstance().getVanishManager().getAllVanished().contains(player.getUniqueId());
    }

    @Override
    public boolean isInGodMode(Player player) {
        return CMI.getInstance().getPlayerManager().getUser(player).isGod();
    }
}
