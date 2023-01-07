package org.kingdoms.services;

import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.entity.Player;

public final class ServiceAuthMe implements ServiceAuth {
    @Override
    public boolean isAuthenticated(Player player) {
        return AuthMeApi.getInstance().isAuthenticated(player);
    }
}
