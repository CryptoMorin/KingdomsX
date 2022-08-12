package org.kingdoms.outposts;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.kingdoms.addons.Addon;
import org.kingdoms.commands.outposts.CommandOutpost;

import java.io.File;

public final class OutpostAddon extends JavaPlugin implements Addon {
    @Override
    public void onDisable() {
        disableAddon();
    }

    @Override
    public void onEnable() {
        registerAddon();
        getLogger().info("Registering commands to kingdoms...");
        new CommandOutpost();
        getLogger().info("Registering event handlers...");
        Bukkit.getPluginManager().registerEvents(new OutpostManager(), this);
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