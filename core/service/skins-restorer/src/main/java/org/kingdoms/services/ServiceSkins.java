package org.kingdoms.services;

import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
import org.bukkit.entity.Player;

public final class ServiceSkins implements Service {
    private static final SkinsRestorerAPI API = SkinsRestorerAPI.getApi();

    public void changeSkin(Player player, SkinValueType type, String value) {
        IProperty prop;
        try {
            if (type == SkinValueType.URL) prop = API.genSkinUrl(value, SkinVariant.CLASSIC);
            else prop = API.getSkinData(value);
        } catch (SkinRequestException e) {
            throw new RuntimeException(e);
        }
        API.applySkin(new PlayerWrapper(player), prop);
    }

    public enum SkinValueType {
        URL, NAME;
    }
}
