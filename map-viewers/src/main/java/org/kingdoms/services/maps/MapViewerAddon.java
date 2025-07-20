package org.kingdoms.services.maps;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.kingdoms.addons.Addon;
import org.kingdoms.commands.admin.CommandAdmin;
import org.kingdoms.config.managers.ConfigManager;
import org.kingdoms.constants.group.flag.GroupBannerRegistry;
import org.kingdoms.locale.LanguageManager;
import org.kingdoms.locale.MessageHandler;
import org.kingdoms.services.managers.SoftService;
import org.kingdoms.services.maps.abstraction.markersets.MarkerType;
import org.kingdoms.services.maps.banners.MinecraftBannerGroupBanner;
import org.kingdoms.utils.config.adapters.YamlResource;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public final class MapViewerAddon extends JavaPlugin implements Addon {
    private static boolean loaded = false;

    private static MapViewerAddon instance;

    public static MapViewerAddon get() {
        return instance;
    }

    private static SoftService[] getSupported() {
        // Don't load the enum before intializing the server.
        return new SoftService[]{SoftService.DYNMAP, SoftService.BLUEMAP, SoftService.SQUAREMAP, SoftService.PL3XMAP};
    }

    protected static boolean isAvailable() {
        return SoftService.anyAvailable(getSupported());
    }

    @Override
    public void reloadAddon() {
        new CommandAdminDynmap(CommandAdmin.getInstance());
        try {
            ServiceMap.load(this);
        } catch (Throwable ex) {
            MessageHandler.sendConsolePluginMessage("&4Failed to load map services&8:");
            ex.printStackTrace();
        }
    }

    @Override
    public void onLoad() {
        if (!isKingdomsLoaded()) return;

        for (SoftService service : getSupported()) {
            if (service.isAvailable()) service.hook(true);
        }
        getLogger().info("Registering kingdoms KingdomBanner type...");
        GroupBannerRegistry.INSTANCE.replace(MinecraftBannerGroupBanner.PROVIDER);
        LanguageManager.registerMessenger(MapViewersLang.class);
        MapThemes.register();
    }

    @Override
    public void onEnable() {
        if (!isKingdomsEnabled()) {
            getLogger().severe("Kingdoms plugin didn't load correctly. Disabling...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        instance = this;
        if (!isAvailable()) {
            throw new IllegalStateException("None of the supported map plugins are installed: " +
                    Arrays.stream(getSupported()).map(SoftService::getName).collect(Collectors.joining(", ")));
        }

        { // maps/map.yml
            MapsConfig.WEB_MAP.load();
            MapsConfig.WEB_MAP.reloadHandle(() -> {
                MessageHandler.sendConsolePluginMessage("&2Performing a full render for web map configs...");
                ServiceMap.load(this);
            });
            ConfigManager.registerAsMainConfig(MapsConfig.WEB_MAP);
            ConfigManager.watch(MapsConfig.WEB_MAP);
        }

        // maps/kingdoms.yml | maps/nations.yml
        for (MarkerType markerType : MarkerType.getMarkerTypes()) {
            Optional<YamlResource> adapterOpt = markerType.getConfigAdapter();
            if (!adapterOpt.isPresent()) continue;

            String name = markerType.getId().substring("kingdoms_".length());
            YamlResource adapter = adapterOpt.get();
            adapter.reloadHandle(() -> {
                MessageHandler.sendConsolePluginMessage("&2Performing a full render for " + name + " markers...");
                ServiceMap.loadMarkerSettings(markerType);
                ServiceMap.load(this);
            });
            ConfigManager.watch(adapter);
        }

        getServer().getPluginManager().registerEvents(new MapEventHandlers(), this);
        reloadAddon();
        registerAddon();
        loaded = true;
    }

    @Override
    public void onDisable() {
        if (!loaded) return;
        getLogger().info("Removing all map markers...");
        ServiceMap.removeEverything();
    }

    @Override
    public String getAddonName() {
        return "map-viewers";
    }

    @Override
    public File getFile() {
        return super.getFile();
    }
}
