package org.kingdoms.services.vanish;

import org.bukkit.entity.Player;
import org.kingdoms.services.Service;

import java.util.UUID;

public interface ServiceCommons extends Service {
    boolean isVanished(Player player);

    boolean isInGodMode(Player player);

    boolean isIgnoring(Player ignorant, UUID ignoring);
}
