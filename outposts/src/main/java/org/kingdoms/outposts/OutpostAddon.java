package org.kingdoms.outposts;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.kingdoms.addons.Addon;
import org.kingdoms.commands.outposts.CommandOutpost;
import org.kingdoms.services.managers.SoftService;

import java.io.File;

public final class OutpostAddon extends JavaPlugin implements Addon {
    @Override
    public void onDisable() {
        getLogger().info("Saving outposts...");
        OutpostDataHandler.saveOutposts();
        disableAddon();
    }

    @Override
    public void onEnable() {
        registerAddon();
        if (!SoftService.WORLD_GUARD.isAvailable()) getLogger().warning("WorldGuard plugin is either not installed or disabled from kingdoms. " +
                "This addon mostly relies on WorldGuard to run events.");

        getLogger().info("Registering commands to kingdoms...");
        new CommandOutpost();
        getLogger().info("Registering event handlers...");
        Bukkit.getPluginManager().registerEvents(new OutpostManager(), this);
        getLogger().info("Loading outposts...");
        OutpostDataHandler.loadOutposts();
    }

    @Override
    public void reloadAddon() {

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