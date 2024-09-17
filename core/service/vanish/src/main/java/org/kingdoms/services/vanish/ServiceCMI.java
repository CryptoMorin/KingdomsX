package org.kingdoms.services.vanish;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class ServiceCMI implements ServiceCommons {
    @Override
    public boolean isVanished(Player player) {
        return CMI.getInstance().getVanishManager().getAllVanished().contains(player.getUniqueId());
    }

    private CMIUser getUser(Player player) {
        return CMI.getInstance().getPlayerManager().getUser(player);
    }

    @Override
    public boolean isInGodMode(Player player) {
        // "This can return NULL in some rare situations, so perform NPE check."
        CMIUser user = getUser(player);
        try {
            return user != null && user.isGod();
        } catch (NoSuchMethodError ex) {
            // Until they update their API.
            /*
            [WARN]: [KingdomsX] Task # for Kingdoms v1.16.3.3 generated an exception java.lang.NoSuchMethodError:
              'java.lang.Boolean com.Zrips.CMI.Containers.CMIUser.isGod()'
                at org.kingdoms.services.vanish.ServiceCMI.isInGodMode(ServiceCMI.java:14) ~[KingdomsX-1.16.3.3.jar:?]
                at org.kingdoms.services.managers.ServiceHandler.lambda$isInGodMode$2(ServiceHandler.java:114)
             */
            return false;
        }
    }

    @Override
    public boolean isIgnoring(Player ignorant, UUID ignoring) {
        CMIUser user = getUser(ignorant);
        return user != null && user.isIgnoring(ignoring);
    }
}
