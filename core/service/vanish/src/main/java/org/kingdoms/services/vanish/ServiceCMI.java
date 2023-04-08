package org.kingdoms.services.vanish;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import org.bukkit.entity.Player;

public final class ServiceCMI implements ServiceVanish {
    @Override
    public boolean isVanished(Player player) {
        return CMI.getInstance().getVanishManager().getAllVanished().contains(player.getUniqueId());
    }

    @Override
    public boolean isInGodMode(Player player) {
        // "This can return NULL in some rare situations, so perform NPE check."
        CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);
        return user != null && user.isGod();
    }
}
