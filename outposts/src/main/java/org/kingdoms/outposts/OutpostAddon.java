package org.kingdoms.outposts;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.kingdoms.addons.Addon;
import org.kingdoms.commands.outposts.CommandOutpost;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.constants.player.KingdomPermission;
import org.kingdoms.gui.GUIConfig;
import org.kingdoms.locale.LanguageManager;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.services.managers.SoftService;

import java.io.File;

public final class OutpostAddon extends JavaPlugin implements Addon {
    private static boolean loaded = false;
    private static OutpostAddon instance;

    @Override
    public void onDisable() {
        if (!loaded) return;
        signalDisable();
        getLogger().info("Saving outposts...");
        OutpostDataHandler.saveOutposts();
        disableAddon();
    }

    public OutpostAddon() {
        instance = this;
    }

    public static OutpostAddon get() {
        return instance;
    }

    public static final KingdomPermission OUTPOST_JOIN_PERMISSION = new KingdomPermission(new Namespace("Outposts", "JOIN"));

    @Override
    public void onLoad() {
        if (!isKingdomsLoaded()) return;
        Kingdoms.get().getPermissionRegistery().register(OUTPOST_JOIN_PERMISSION);
        Kingdoms.get().getAuditLogRegistry().register(LogKingdomOutpostJoin.PROVIDER);
        LanguageManager.registerMessenger(OutpostsLang.class);
    }

    @Override
    public void onEnable() {
        if (!isKingdomsEnabled()) {
            getLogger().severe("Kingdoms plugin didn't load correctly. Disabling...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (!SoftService.WORLD_GUARD.isAvailable())
            getLogger().warning("WorldGuard plugin is either not installed or disabled from kingdoms. " +
                    "This addon mostly relies on WorldGuard to run events.");

        getLogger().info("Registering commands to kingdoms...");
        getLogger().info("Registering event handlers...");
        Bukkit.getPluginManager().registerEvents(new OutpostManager(), this);

        reloadAddon();
        registerAddon();
        GUIConfig.loadInternalGUIs(this);
        loaded = true;
    }

    @Override
    public void reloadAddon() {
        new CommandOutpost();
        OutpostDataHandler.loadOutposts();
    }

    @Override
    public String getAddonName() {
        return "outposts";
    }

    @Override
    public File getFile() {
        return super.getFile();
    }
}