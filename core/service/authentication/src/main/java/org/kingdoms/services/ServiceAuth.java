package org.kingdoms.services;

import org.bukkit.entity.Player;

public interface ServiceAuth extends Service {
    boolean isAuthenticated(Player player);
}
