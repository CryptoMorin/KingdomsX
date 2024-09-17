package org.kingdoms.peacetreaties;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.kingdoms.addons.Addon;
import org.kingdoms.config.managers.ConfigManager;
import org.kingdoms.config.managers.ConfigWatcher;
import org.kingdoms.constants.metadata.KingdomMetadataHandler;
import org.kingdoms.constants.metadata.KingdomMetadataRegistry;
import org.kingdoms.gui.GUIConfig;
import org.kingdoms.locale.LanguageManager;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.managers.fsck.HealthCheckupHandler;
import org.kingdoms.peacetreaties.commands.CommandPeaceTreaty;
import org.kingdoms.peacetreaties.config.CustomConfigValidators;
import org.kingdoms.peacetreaties.config.PeaceTreatyConfig;
import org.kingdoms.peacetreaties.config.PeaceTreatyLang;
import org.kingdoms.peacetreaties.data.*;
import org.kingdoms.peacetreaties.managers.PeaceTreatyFSCK;
import org.kingdoms.peacetreaties.managers.RelationshipListener;
import org.kingdoms.peacetreaties.managers.TermManager;
import org.kingdoms.peacetreaties.managers.WarPointManager;
import org.kingdoms.peacetreaties.terms.TermRegistry;
import org.kingdoms.peacetreaties.terms.types.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class PeaceTreatiesAddon extends JavaPlugin implements Addon {
    private static boolean loaded = false;
    private static PeaceTreatiesAddon instance;
    private final TermRegistry termRegistry = new TermRegistry();

    public TermRegistry getTermRegistry() {
        return termRegistry;
    }

    public PeaceTreatiesAddon() {
        instance = this;
    }

    public static PeaceTreatiesAddon get() {
        return instance;
    }

    private final Set<KingdomMetadataHandler> metadataHandlers = new HashSet<>();

    @Override
    public void uninstall() {
        getLogger().info("Removing peace treaties metadata info...");
        KingdomMetadataRegistry.removeMetadata(Kingdoms.get().getDataCenter().getKingdomManager(), metadataHandlers);
    }

    @Override
    public void onLoad() {
        if (!isKingdomsLoaded()) return;

        getLogger().info("Registering kingdoms metadata handler...");

        metadataHandlers.addAll(Arrays.asList(PeaceTreatyProposerMetaHandler.INSTANCE, PeaceTreatyReceiverMetaHandler.INSTANCE, WarPointsMetaHandler.INSTANCE));
        metadataHandlers.forEach(x -> Kingdoms.get().getMetadataRegistry().register(x));

        Kingdoms.get().getAuditLogRegistry().register(LogPeaceTreatySent.PROVIDER);
        Kingdoms.get().getAuditLogRegistry().register(LogPeaceTreatyReceived.PROVIDER);

        getLogger().info("Registering default terms...");
        Arrays.asList(TakeMoneyTerm.PROVIDER, LeaveDisbandNationTerm.PROVIDER, TakeResourcePointsTerm.PROVIDER,
                        ScutageTerm.PROVIDER, AnnulTreatiesTerm.PROVIDER, MiscUpgradesTerm.PROVIDER, KeepLandsTerm.PROVIDER,
                        LimitTurretsTerm.PROVIDER, LimitStructuresTerm.PROVIDER, LimitClaimsTerm.PROVIDER, KingChangeTerm.PROVIDER)
                .forEach(termRegistry::register);

        PeaceTreatiesPlaceholder.init();
        LanguageManager.registerMessenger(PeaceTreatyLang.class);
        CustomConfigValidators.init();
        ConfigManager.registerAsMainConfig(PeaceTreatyConfig.PEACE_TREATIES);
    }

    @Override
    public void onEnable() {
        if (!isKingdomsEnabled()) {
            getLogger().severe("Kingdoms plugin didn't load correctly. Disabling...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        TermRegistry.loadTermGroupings();
        Bukkit.getPluginManager().registerEvents(new WarPointManager(), this);
        Bukkit.getPluginManager().registerEvents(new RelationshipListener(), this);
        Bukkit.getPluginManager().registerEvents(new TermManager(), this);

        new CommandPeaceTreaty();

        // peace-treaties.yml
        ConfigWatcher.register(PeaceTreatyConfig.PEACE_TREATIES.getFile().toPath().getParent(), ConfigWatcher::handleNormalConfigs);
        ConfigManager.registerNormalWatcher("peace-treaties.yml", (event) -> {
            ConfigWatcher.reload(PeaceTreatyConfig.PEACE_TREATIES, "peace-treaties.yml");
            getLogger().info("Reloading terms...");
            TermRegistry.loadTermGroupings();
        });

        GUIConfig.loadInternalGUIs(this);

        registerAddon();
        HealthCheckupHandler.addCheckupHandler(new PeaceTreatyFSCK());
        loaded = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        signalDisable();
    }

    @Override
    public void reloadAddon() {
        PeaceTreatyConfig.getConfig().reload();
        TermRegistry.loadTermGroupings();
        new CommandPeaceTreaty();
    }

    @Override
    public String getAddonName() {
        return "peace-treaties";
    }

    @Override
    public File getFile() {
        return super.getFile();
    }
}