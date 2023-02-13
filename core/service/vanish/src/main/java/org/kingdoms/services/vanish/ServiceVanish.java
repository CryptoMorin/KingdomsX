package org.kingdoms.services.vanish;

import org.bukkit.entity.Player;
import org.kingdoms.services.Service;

public interface ServiceVanish extends Service {
    boolean isVanished(Player player);
    boolean isInGodMode(Player player);
}
