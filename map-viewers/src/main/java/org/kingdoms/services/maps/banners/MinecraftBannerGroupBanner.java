package org.kingdoms.services.maps.banners;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.block.banner.Pattern;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.constants.group.flag.GroupBannerProvider;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.locale.KingdomsLang;
import org.kingdoms.managers.chat.ChatInputManager;
import org.kingdoms.services.maps.MapsConfig;
import org.kingdoms.utils.Banners;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class MinecraftBannerGroupBanner extends org.kingdoms.constants.group.flag.MinecraftBannerGroupBanner {
    public static final Provider PROVIDER = new Provider();

    public static class Provider implements GroupBannerProvider<MinecraftBannerGroupBanner> {
        @Override
        public MinecraftBannerGroupBanner construct() {
            return new MinecraftBannerGroupBanner();
        }

        @Override
        public CompletableFuture<MinecraftBannerGroupBanner> prompt(Player player) {
            KingdomsLang.GROUP_BANNER_MINECRAFT_BANNER_PROMPT.sendMessage(player);

            return ChatInputManager.awaitInput(player, input -> {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (!Banners.isBanner(item)) {
                    KingdomsLang.GROUP_BANNER_MINECRAFT_BANNER_NOT_A_BANNER.sendError(player);
                    return null;
                }

                MinecraftBannerGroupBanner banner = construct();
                banner.setMaterial(XMaterial.matchXMaterial(item));
                banner.setPatterns(Banners.getBannerPatterns(item));
                return banner;
            });
        }

        @Override
        public @NonNull Namespace getNamespace() {
            return org.kingdoms.constants.group.flag.MinecraftBannerGroupBanner.PROVIDER.getNamespace();
        }
    }

    private BufferedImage cachedImage;

    @Override
    public void setPatterns(List<Pattern> patterns) {
        super.setPatterns(patterns);
        cachedImage = null;
    }

    @Override
    public void setMaterial(XMaterial material) {
        super.setMaterial(material);
        cachedImage = null;
    }

    @Override
    public BufferedImage asImage() {
        if (cachedImage == null) {
            int scaling = MapsConfig.BANNERS_SCALING.getManager().getInt();
            if (scaling <= 0) scaling = 2;
            cachedImage = BannerImage.bannerToImage(getMaterial(), getPatterns(), scaling);
        }

        return cachedImage;
    }

    @Override
    public Dimension getImageSize() {
        BufferedImage img = asImage();
        return new Dimension(img.getWidth(), img.getHeight());
    }
}
