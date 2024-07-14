package org.kingdoms.services;

import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class ServiceSkins implements Service {
    private static final SkinsRestorer API = SkinsRestorerProvider.get();

    public void changeSkin(Player player, SkinValueType type, String value) {
        // Example plugin: https://github.com/SkinsRestorer/SkinsRestorerAPIExample/blob/main/src/main/java/net/skinsrestorer/apiexample/SkinsRestorerAPIExample.java
        SkinProperty prop;
        try {
            PlayerStorage playerStorage = API.getPlayerStorage();
            Optional<SkinProperty> property = playerStorage.getSkinForPlayer(player.getUniqueId(), player.getName());

            SkinStorage skinStorage = API.getSkinStorage();
            if (type == SkinValueType.URL) {
                prop = skinStorage.findOrCreateSkinData(value, SkinVariant.CLASSIC).get().getProperty();
            } else prop = property.get();
        } catch (DataRequestException | MineSkinException e) {
            throw new RuntimeException("Error while attemptin gto change " + player + "'s skin to '" + value + "' value of " + type, e);
        }
        API.getSkinApplier(Player.class).applySkin(player, prop);
    }

    public enum SkinValueType {
        URL, NAME;
    }
}
