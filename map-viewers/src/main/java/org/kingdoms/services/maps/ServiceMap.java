package org.kingdoms.services.maps;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kingdoms.config.KingdomsConfig;
import org.kingdoms.constants.group.Group;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.group.Nation;
import org.kingdoms.constants.group.flag.GroupBanner;
import org.kingdoms.constants.group.model.relationships.KingdomRelation;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.land.location.SimpleChunkLocation;
import org.kingdoms.constants.land.location.SimpleLocation;
import org.kingdoms.constants.land.structures.Structure;
import org.kingdoms.constants.land.structures.type.StructureTypePowercell;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.constants.themes.MainTheme;
import org.kingdoms.constants.top.common.KingdomTopData;
import org.kingdoms.data.Pair;
import org.kingdoms.data.centers.KingdomsDataCenter;
import org.kingdoms.data.centers.KingdomsStartup;
import org.kingdoms.locale.compiler.MessageCompiler;
import org.kingdoms.locale.compiler.MessageObject;
import org.kingdoms.locale.compiler.builders.MessageObjectBuilder;
import org.kingdoms.locale.compiler.builders.MessageObjectLinker;
import org.kingdoms.locale.compiler.builders.context.HTMLMessageBuilderContextProvider;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.main.KLogger;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.managers.invasions.Invasion;
import org.kingdoms.scheduler.DelayedRepeatingTask;
import org.kingdoms.services.managers.ServiceHandler;
import org.kingdoms.services.managers.SoftService;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;
import org.kingdoms.services.maps.abstraction.markersets.MarkerConfig;
import org.kingdoms.services.maps.abstraction.markersets.MarkerListenerType;
import org.kingdoms.services.maps.abstraction.markersets.MarkerType;
import org.kingdoms.services.maps.abstraction.markersets.MarkerTypeRegistry;
import org.kingdoms.services.maps.banners.BannerImage;
import org.kingdoms.services.maps.banners.MinecraftBannerGroupBanner;
import org.kingdoms.services.maps.bluemap.ServiceBlueMap;
import org.kingdoms.services.maps.dynmap.ServiceDynmap;
import org.kingdoms.services.maps.pl3xmap.ServicePl3xMap;
import org.kingdoms.services.maps.squaremap.ServiceSquaremap;
import org.kingdoms.utils.ColorUtils;
import org.kingdoms.utils.config.ConfigSection;
import org.kingdoms.utils.debugging.DebugNS;
import org.kingdoms.utils.debugging.KingdomsDebug;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class ServiceMap {
    public static final Collection<MapServiceProvider> APIS;
    public static final DebugNS DEBUG_NS = KingdomsDebug.register("MAP_VIEWERS");
    private static DelayedRepeatingTask reloadingTask;
    public static boolean hasLoaded = false;

    static {
        loadMarkersSettings();
        Logger logger = MapViewerAddon.get().getLogger();

        List<MapServiceProvider> apis = new ArrayList<>(1);
        if (SoftService.DYNMAP.isAvailable()) apis.add(new MapServiceProvider(new ServiceDynmap(logger)));
        if (SoftService.SQUAREMAP.isAvailable()) apis.add(new MapServiceProvider(new ServiceSquaremap(logger)));
        if (SoftService.PL3XMAP.isAvailable()) apis.add(new MapServiceProvider(new ServicePl3xMap(logger)));
        if (SoftService.BLUEMAP.isAvailable()) {
            ServiceBlueMap.createWhenAvailable(logger, () -> {
                MapServiceProvider provider = new MapServiceProvider(new ServiceBlueMap(logger));
                apis.add(provider);
                if (hasLoaded) provider.load(MapViewerAddon.get());
            });
        }

        APIS = apis; // We can't optimize this list because some services are delayed like Dynmap and BlueMap
        if (SoftService.DYNMAP.isAvailable()) ServiceHandler.addGlobalMessageHandler(ServiceMap::sendGlobalMessage);
    }

    public static void loadMarkersSettings() {
        MarkerType.KINGDOMS.listen(new MarkerListenerType.Kingdoms().showLands(true).showIcons(true)
                .updateListener(kingdom -> ServiceMap.updateLands(MarkerType.KINGDOMS, kingdom, true, APIS)));

        MarkerType.NATIONS.listen(new MarkerListenerType.Nations(true, true));

        MarkerType.POWERCELL.listen(new MarkerListenerType.Kingdoms().showLands(false).showIcons(false)
                .updateListener(kingdom -> updatePowercellMarkers(APIS, kingdom)));

        ConfigSection kingdomConfig = MarkerType.KINGDOMS.getConfig();
        KingdomsDataCenter dataCenter = Kingdoms.get().getDataCenter();

        for (Map.Entry<String, KingdomTopData> top : dataCenter.getKingdomManager().getTopData().entrySet()) {
            MarkerType marker = new MarkerType(
                    "KINGDOM_TOP_" + top.getKey().toUpperCase(Locale.ENGLISH).replace('-', '_'),
                    MarkerType.KINGDOMS,
                    kingdomConfig.getSection("top-kingdoms", top.getKey())
            );
            int topPositions = marker.getConfig().getInt("top");
            MarkerTypeRegistry.INSTANCE.register(marker.listen(new MarkerListenerType.Kingdoms()
                    .showIcons(true)
                    .showLands(true)
                    .updateListener(kingdom -> ServiceMap.updateLands(marker, kingdom, true, APIS))
                    .ignore(kingdom -> top.getValue().getPositionOf(kingdom).orElse(Integer.MAX_VALUE) > topPositions)));
        }

        for (MarkerType markerType : MarkerType.getMarkerTypes()) {
            loadMarkerSettings(markerType);
        }
    }

    public static void loadMarkerSettings(MarkerType markerType) {
        markerType.getMarkerSettings()
                .persistent(MarkerConfig.PERSISTENT_DATA.getManager(markerType).getBoolean())
                .hideByDefault(MarkerConfig.HIDE_BY_DEFAULT.getManager(markerType).getBoolean())
                .showControls(MarkerConfig.SHOW_CONTROLS.getManager(markerType).getBoolean())
                .zIndex(MarkerConfig.Z_INDEX.getManager(markerType).getInt())
                .priority(MarkerConfig.PRIORITY.getManager(markerType).getInt())
                .label(MarkerConfig.LABEL.getManager(markerType).getString())
                .sorting(MarkerConfig.SORTING.getManager(markerType).getInt())
                .disabledWorlds(world -> !isEnabledInWorld(world));
    }

    public static void load(Plugin plugin) {
        KingdomsStartup.whenReady(pl -> {
            hasLoaded = true;
            KLogger.debug(DEBUG_NS, "Startup finished, loading map data...");

            // We use toArray() here because there's a slight possibility of ConcurrentModificationException
            for (MapServiceProvider api : APIS.toArray(new MapServiceProvider[0])) api.load(plugin);

            Duration interval = MapsConfig.UPDATE_INTERVAL.getManager().getTime();
            if (interval == null || interval.toMillis() > 0) return;

            if (reloadingTask != null) reloadingTask.cancel();
            reloadingTask = Kingdoms.taskScheduler().async().repeating(interval, interval, () -> {
                MapViewerAddon.get().getLogger().info("Performing a full render...");
                fullRender();
                update();
            });
        });
    }

    public static void removeEverything() {
        KLogger.debug(DEBUG_NS, "Removing all map data...");
        for (MapServiceProvider api : APIS) {
            api.removeEverything();
        }
    }

    public static void sendGlobalMessage(Player player, String message) {
        for (MapServiceProvider api : APIS) {
            api.sendGlobalMessage(player, message);
        }
    }

    public static int fullRender() {
        return fullRender(APIS);
    }

    static int fullRender(Collection<MapServiceProvider> apis) {
        for (MapServiceProvider api : apis) {
            api.removeEverything();
        }

        int totalLands = 0;
        if (MarkerType.KINGDOMS.isEnabled()) {
            for (Kingdom kingdom : KingdomsDataCenter.get().getKingdomManager().getKingdoms()) {
                if (kingdom.isHidden()) continue;
                totalLands += updateLands(MarkerType.KINGDOMS, kingdom, false, apis);
            }
        }
        if (MarkerType.NATIONS.isEnabled()) {
            for (Nation nation : KingdomsDataCenter.get().getNationManager().getNations()) {
                if (nation.isHidden()) continue;
                updateLands(MarkerType.NATIONS, nation, false, apis);
            }
        }

        registerPowercellMarkers(apis, null);
        updateTopMarkers(apis);
        update();

        KLogger.debug(DEBUG_NS, "Requested a full render for APIs: " + apis + " and " + totalLands + " lands were updated.");
        return totalLands;
    }

    public static void updateLands(Kingdom kingdom) {
        for (Pair<MarkerType, MarkerListenerType.Kingdoms> markerType : MarkerType.<MarkerListenerType.Kingdoms>getMarkerTypes(MarkerListenerType.KINGDOMS)) {
            if (markerType.getValue().updateListener == null) continue;
            if (!markerType.getKey().isEnabled()) continue;
            if (markerType.getValue().isIgnored(kingdom)) continue;
            markerType.getValue().onUpdate(kingdom);
        }
    }

    public static void updateLands(Nation nation) {
        for (Pair<MarkerType, MarkerListenerType.Nations> markerType : MarkerType.<MarkerListenerType.Nations>getMarkerTypes(MarkerListenerType.NATIONS)) {
            if (!markerType.getValue().showLands) continue;
            if (!markerType.getKey().isEnabled()) continue;
            updateLands(markerType.getKey(), nation, true, APIS);
        }
    }

    public static void removePowercellMarkers(Structure powercell) {
        MarkerType marker = MarkerType.POWERCELL;
        Land land = powercell.getLand();
        Kingdom kingdom = land.getKingdom();
        if (kingdom.isHidden()) return;

        APIS.forEach(x -> x.removeLands(marker, kingdom));
        registerPowercellMarkers(APIS, struct -> struct == powercell, kingdom, marker);
    }

    public static void updatePowercellMarkers(Structure powercell, int level) {
        MarkerType marker = MarkerType.POWERCELL;
        Land land = powercell.getLand();
        Kingdom kingdom = land.getKingdom();
        if (kingdom.isHidden()) return;

        APIS.forEach(x -> x.removeLands(marker, kingdom));
        registerPowercellMarkers(APIS, struct -> struct == powercell, kingdom, marker);

        SimpleChunkLocation[] powercellProtectedLands = land.getLocation().getChunksAround(level, true);
        LandMarkerSettings settings = getLandMarkerSettings(marker, false, kingdom, powercell.getMessageContext());

        Map<String, Collection<SimpleChunkLocation>> lands =
                groupChunks(Arrays.stream(powercellProtectedLands).collect(Collectors.toList()));

        for (MapServiceProvider api : APIS) {
            api.addLands(marker, kingdom, lands, settings, toId(powercell.getOrigin()));
            api.updateStructure(marker, powercell);
        }
    }

    private static String toId(SimpleLocation location) {
        return location.getX() + "," + location.getY() + "," + location.getZ();
    }

    private static void registerPowercellMarkers(Collection<MapServiceProvider> apis, Predicate<Structure> ignore) {
        MarkerType marker = MarkerType.POWERCELL;
        if (!marker.isEnabled()) return;
        apis.forEach(x -> x.removeLands(marker));

        for (Kingdom kingdom : KingdomsDataCenter.get().getKingdomManager().getKingdoms()) {
            if (kingdom.isHidden()) continue;
            registerPowercellMarkers(apis, ignore, kingdom, marker);
        }
    }

    private static void updatePowercellMarkers(Collection<MapServiceProvider> apis, Kingdom kingdom) {
        if (kingdom.isHidden()) return;

        MarkerType marker = MarkerType.POWERCELL;
        if (!marker.isEnabled()) return;

        apis.forEach(x -> x.removeLands(marker));
        registerPowercellMarkers(apis, null, kingdom, marker);
    }

    private static void registerPowercellMarkers(Collection<MapServiceProvider> apis, Predicate<Structure> ignore, Kingdom kingdom, MarkerType marker) {
        for (Land land : kingdom.getLands()) {
            Structure powercell = land.getStructure(struct -> struct.getStyle().getType() instanceof StructureTypePowercell);

            if (powercell != null) {
                if (ignore != null && ignore.test(powercell)) continue;

                SimpleChunkLocation[] powercellProtectedLands = land.getLocation().getChunksAround(powercell.getLevel(), true);
                LandMarkerSettings settings = getLandMarkerSettings(marker, false, kingdom, powercell.getMessageContext());
                Map<String, Collection<SimpleChunkLocation>> lands =
                        groupChunks(Arrays.stream(powercellProtectedLands).collect(Collectors.toList()));

                for (MapServiceProvider api : apis) {
                    api.addLands(marker, kingdom, lands, settings, toId(powercell.getOrigin()));
                    api.updateStructure(marker, powercell);
                }
            }
        }
    }

    private static void updateTopMarkers(Collection<MapServiceProvider> apis) {
        KingdomsDataCenter dataCenter = Kingdoms.get().getDataCenter();
        for (Map.Entry<String, KingdomTopData> top : dataCenter.getKingdomManager().getTopData().entrySet()) {
            String markerName = "KINGDOM_TOP_" + top.getKey().toUpperCase(Locale.ENGLISH).replace('-', '_');
            MarkerType marker = MarkerTypeRegistry.INSTANCE.getRegistered(Namespace.kingdoms(markerName));
            if (marker == null || !marker.isEnabled()) continue;
            int topPositions = marker.getConfig().getInt("top");

            for (Kingdom topKingdom : top.getValue().getTop(0, topPositions, null)) {
                updateLands(marker, topKingdom, true, apis);
            }
        }
    }

    static int updateLands(MarkerType marker, Kingdom kingdom, boolean update, Collection<MapServiceProvider> apis) {
        Map<String, Collection<SimpleChunkLocation>> lands = groupLands(marker, kingdom.getLands(), apis);
        LandMarkerSettings settings = getLandMarkerSettings(marker, false, kingdom, null);

        for (MapServiceProvider api : apis) {
            api.updateLands(marker, kingdom, lands, settings);
            if (update) api.update();
        }

        return lands.size();
    }

    private static @NotNull Map<String, Collection<SimpleChunkLocation>> groupLands(@Nullable MarkerType iconMarkerType,
                                                                                    Collection<Land> lands,
                                                                                    Collection<MapServiceProvider> apis) {
        Map<String, Collection<SimpleChunkLocation>> mappedLands = new HashMap<>(3);
        for (Land land : lands) {
            if (iconMarkerType != null) {
                for (MapServiceProvider api : apis) api.addIconsOf(iconMarkerType, land);
            }

            mappedLands.compute(land.getLocation().getWorld(), (k, v) -> {
                if (v == null) v = new ArrayList<>(10);
                v.add(land.getLocation());
                return v;
            });
        }
        return mappedLands;
    }

    private static @NotNull Map<String, Collection<SimpleChunkLocation>> groupChunks(Collection<SimpleChunkLocation> lands) {
        Map<String, Collection<SimpleChunkLocation>> mappedLands = new HashMap<>(3);
        for (SimpleChunkLocation chunk : lands) {
            mappedLands.compute(chunk.getWorld(), (k, v) -> {
                if (v == null) v = new ArrayList<>(10);
                v.add(chunk);
                return v;
            });
        }
        return mappedLands;
    }

    static int updateLands(MarkerType marker, Nation nation, boolean update, Collection<MapServiceProvider> apis) {
        Map<String, ? extends Collection<SimpleChunkLocation>> lands = nation.getLands(true);
        LandMarkerSettings settings = getLandMarkerSettings(marker, false, nation, null);

        for (MapServiceProvider api : apis) {
            api.updateLands(marker, nation, lands, settings);
            if (update) api.update();
        }

        return lands.size();
    }

    public static boolean isEnabledInWorld(org.kingdoms.server.location.World world) {
        return !KingdomsConfig.DISABLED_WORLDS.isInDisabledWorld(world) &&
                !KingdomsConfig.Claims.DISABLED_WORLDS.getManager().getStringList().contains(world.getName());
    }

    public static void clearLands(MarkerType markerType, @NotNull Group group) {
        KLogger.debug(DEBUG_NS, "Requested land marker cleanup for " + group.getClass().getSimpleName() + " of " + group.getName());
        for (MapServiceProvider api : APIS) api.removeLands(markerType, group);
    }

    public static void update() {
        for (MapServiceProvider api : APIS) api.update();
    }

    public static void removeStructureIcon(MarkerType type, @NotNull Structure structure) {
        removeIcon(type, structure.getOrigin());
    }

    public static void removeIcon(MarkerType type, SimpleLocation location) {
        String hashed = MapServiceProvider.iconIdOf(location);
        for (MapServiceProvider api : APIS) api.removeIcon(type, hashed, location);
    }

    public static void removeHomeIcon(MarkerType type, @NotNull Location location) {
        Objects.requireNonNull(location, "Cannot remove icon from null location");
        removeIcon(type, SimpleLocation.of(location));
    }

    public static void updateIcon(MarkerType markerType, @NotNull SimpleLocation location, @NotNull String iconName,
                                  @Nullable MessagePlaceholderProvider context) {
        String id = MapServiceProvider.iconIdOf(location);
        LandMarkerSettings iconSettings = MapServiceProvider.getIconSettings(iconName, context);
        if (iconSettings == null) return;

        for (MapServiceProvider api : APIS) {
            api.updateIcon(markerType, location, iconName, id, iconSettings);
        }
    }

    public static void updateStructure(MarkerType markerType, @NotNull Structure structure) {
        for (MapServiceProvider api : APIS) {
            api.updateStructure(markerType, structure);
        }
    }

    public static void updateHomeIcon(MarkerType markerType, @NotNull Location home, boolean national) {
        updateIcon(markerType, SimpleLocation.of(home), national ? "national-spawn" : "home", null);
    }

    public static void clearInvasionAreas(Invasion invasion) {
        for (MapServiceProvider api : APIS) api.clearInvasionAreas(invasion);
    }

    public static void invasionStart(Invasion invasion) {
        World world = invasion.getStartLocation().getWorld();
        Kingdom defender = invasion.getDefender();
        Set<SimpleChunkLocation> lands = invasion.getAffectedLands();

        LandMarkerSettings settings = getLandMarkerSettings(MarkerType.KINGDOMS, true, defender, null);
        for (MapServiceProvider api : APIS)
            api.invasionStart(MarkerType.KINGDOMS, world, defender, invasion, lands, settings);
    }

    private static String buildDescription(MarkerType markerType, @NotNull Group group, String description,
                                           @Nullable MessagePlaceholderProvider context) {
        MessageObject memberObj = MessageCompiler.compile(MarkerConfig.DESCRIPTIONS_MEMBERS.getManager(markerType).getString());
        MessageObjectLinker members = new MessageObjectLinker();

        if (group instanceof Kingdom) {
            int maxEntries = 20;
            List<KingdomPlayer> playerMembers = group.getKingdomPlayers();
            playerMembers.sort(KingdomPlayer::compareTo);
            int remaining = playerMembers.size() - maxEntries;

            for (KingdomPlayer member : playerMembers) {
                if (maxEntries-- <= 0) {
                    MessageObject membersEtc = MessageCompiler.compile(MarkerConfig.DESCRIPTIONS_MEMBERS_ETC.getManager(markerType).getString());
                    members.add(membersEtc, new MessagePlaceholderProvider().withContext(group).raw("remaining", remaining));
                    break;
                }
                MessagePlaceholderProvider settings = new MessagePlaceholderProvider().withContext(member.getOfflinePlayer());
                HTMLMessageBuilderContextProvider.addHandler(settings);
                members.add(memberObj, settings);
                // For some reasons \n isn't being properly translated to <br> it just stays as \n
                // members.add("\n");
            }
        } else if (group instanceof Nation) {
            for (Kingdom kingdom : ((Nation) group).getKingdoms()) {
                members.add(memberObj, new MessagePlaceholderProvider().withContext(kingdom));
            }
        }

        String alliesStr = MarkerConfig.DESCRIPTIONS_ALLIES.getManager(markerType).getString();
        String trucesStr = MarkerConfig.DESCRIPTIONS_TRUCES.getManager(markerType).getString();
        String enemiesStr = MarkerConfig.DESCRIPTIONS_ENEMIES.getManager(markerType).getString();

        StringBuilder allies = new StringBuilder();
        StringBuilder truces = new StringBuilder();
        StringBuilder enemies = new StringBuilder();

        for (Map.Entry<UUID, KingdomRelation> relation : group.getRelations().entrySet()) {
            Kingdom other = Kingdom.getKingdom(relation.getKey());

            KingdomRelation rel = relation.getValue();
            StringBuilder builder;
            String str;
            switch (rel) {
                case ALLY:
                    builder = allies;
                    str = alliesStr;
                    break;
                case TRUCE:
                    builder = truces;
                    str = trucesStr;
                    break;
                case ENEMY:
                    builder = enemies;
                    str = enemiesStr;
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected relation in maps description builder: " + rel + " for kingdom " + group.getName() + " - " + other.getName());
            }

            MessageObjectBuilder messageObjectBuilder = MessageCompiler.compile(str);
            MessagePlaceholderProvider settings = new MessagePlaceholderProvider().withContext(other);
            builder.append(messageObjectBuilder.buildPlain(settings));
        }

        String markerFlag;
        GroupBanner banner = group.getFlag();
        if (banner == null) banner = group.getBanner();

        double flagHeight, flagWidth;
        boolean isBanner = banner instanceof MinecraftBannerGroupBanner;
        if (banner == null) {
            markerFlag = "";
            flagWidth = flagHeight = 0;
        } else {
            markerFlag = MarkerConfig.DESCRIPTIONS_FLAG.getManager(markerType).getString();

            // TODO where are custom images saved? Maybe use <img src="data:image/png;base64, ?
            //      But using Base64 is not practical because they're mostly big.

            String src;
            switch (banner.getPreferredMethod()) {
                case URL:
                    URL url = banner.urlOrCreate(null);
                    src = url.toString();
                    Dimension dim = banner.getImageSize();
                    if (dim == null) {
                        flagWidth = flagHeight = 0;
                    } else {
                        flagWidth = dim.getWidth();
                        flagHeight = dim.getHeight();
                    }
                    break;
                case IMAGE:
                    BufferedImage image = banner.asImage();
                    src = BannerImage.embedImgSrc(image, "png");
                    flagWidth = image.getWidth();
                    flagHeight = image.getHeight();
                    break;
                default:
                    throw new AssertionError("Unknown preferred method for banner: " + banner.getPreferredMethod());
            }

            markerFlag = markerFlag.replace("%src%", src);
        }

        MessagePlaceholderProvider settings = (context == null ? new MessagePlaceholderProvider() : context)
                .withContext(group)
                .raws(
                        "flag_width", flagWidth,
                        "flag_height", flagHeight,
                        "flag_is_banner", isBanner,

                        "members", members,

                        "allies", allies,
                        "truces", truces,
                        "enemies", enemies,

                        "has_allies", allies.length() != 0,
                        "has_truces", truces.length() != 0,
                        "has_enemies", enemies.length() != 0
                ).parse("flag", markerFlag);

        HTMLMessageBuilderContextProvider.addHandler(settings);

        MessageObjectBuilder messageObjectBuilder = MessageCompiler.compile(description);
        return messageObjectBuilder.buildPlain(settings);
    }

    public static LandMarkerSettings getLandMarkerSettings(MarkerType markerType, boolean invasion, Group group,
                                                           @Nullable MessagePlaceholderProvider context) {
        Color fillColor = group.getColors().get(MapThemes.FILL);
        Color lineColor = group.getColors().get(MapThemes.OUTLINE);

        if (fillColor == null) {
            fillColor = group.getColors().get(MainTheme.INSTANCE);
        }
        if (lineColor == null) {
            lineColor = group.getColors().get(MainTheme.INSTANCE);
        }

        int lineWeight;
        int fillOpacity;

        if (invasion) {
            lineColor = ColorUtils.parseColor(MarkerConfig.INVASION_LINE_COLOR.getManager(markerType).getString());
            lineWeight = MarkerConfig.INVASION_LINE_WEIGHT.getManager(markerType).getInt();

            fillOpacity = MarkerConfig.INVASION_FILL_OPACITY.getManager(markerType).getInt();
            fillColor = ColorUtils.parseColor(MarkerConfig.INVASION_FILL_COLOR.getManager(markerType).getString());
        } else {
            lineColor = markerType.config(MarkerConfig.LINE_FORCE).getBoolean() || lineColor == null ?
                    ColorUtils.parseColor(MarkerConfig.LINE_COLOR.getManager(markerType).getString()) : lineColor;
            lineWeight = MarkerConfig.LINE_WEIGHT.getManager(markerType).getInt();

            fillOpacity = MarkerConfig.FILL_OPACITY.getManager(markerType).getInt();
            fillColor = markerType.config(MarkerConfig.FILL_FORCE).getBoolean() || fillColor == null ?
                    ColorUtils.parseColor(MarkerConfig.FILL_COLOR.getManager(markerType).getString()) : fillColor;
        }

        fillColor = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), fillOpacity);

        return new LandMarkerSettings()
                .priority(markerType.config(MarkerConfig.Z_INDEX).getInt())
                .fillColor(fillColor)
                .stroke(lineColor, lineWeight)
                .clickDescription(buildDescription(markerType, group, MarkerConfig.DESCRIPTIONS_CLICK.getManager(markerType).getString(), context))
                .hoverDescription(buildDescription(markerType, group, MarkerConfig.DESCRIPTIONS_HOVER.getManager(markerType).getString(), context))
                .zoom(MarkerConfig.ZOOM_MIN.getManager(markerType).getInt(), MarkerConfig.ZOOM_MAX.getManager(markerType).getInt())
                .specialFlag(false);
    }
}
