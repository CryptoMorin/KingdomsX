package org.kingdoms.services.maps;

import com.google.common.base.Strings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kingdoms.config.implementation.YamlConfigAccessor;
import org.kingdoms.constants.group.Group;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.group.Nation;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.land.location.SimpleChunkLocation;
import org.kingdoms.constants.land.location.SimpleLocation;
import org.kingdoms.constants.land.structures.Structure;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.locale.messenger.StaticMessenger;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.managers.invasions.Invasion;
import org.kingdoms.platform.bukkit.adapters.BukkitAdapter;
import org.kingdoms.server.location.Vector3Location;
import org.kingdoms.services.maps.abstraction.MapAPI;
import org.kingdoms.services.maps.abstraction.markers.IconMarker;
import org.kingdoms.services.maps.abstraction.markers.LandMarkerSettings;
import org.kingdoms.services.maps.abstraction.markersets.MarkerType;
import org.kingdoms.utils.chunks.ChunkConnections;

import java.util.*;

public final class MapServiceProvider {
    private final MapAPI api;

    public static final String FOLDER = "maps", MAIN_CONFIG_NAME = "map.yml";
    private final Map<MarkerType, MapLandManager> landManagers = new IdentityHashMap<>(5);
    private final IconFolderRegistry iconRegistry;

    public MapServiceProvider(MapAPI api) {
        this.api = api;
        iconRegistry = new IconFolderRegistry(
                "Map Icons",
                Kingdoms.getPath(FOLDER).resolve("icons"),
                FOLDER + "/icons",
                api
        );
        iconRegistry.copyDefaults(true);
    }

    protected void load(Plugin plugin) {
        if (MapsConfig.ICONS_ENABLED.getManager().getBoolean()) loadIcons();
        ServiceMap.fullRender(Collections.singleton(this));
    }

    public void removeEverything() {
        removeAllLands();
        api.removeEverything();
    }

    public void removeAllLands() {
        landManagers.values().forEach(MapLandManager::clearAreasAndDeleteMarkers);
    }

    protected void removeLands(MarkerType markerType) {
        MapLandManager polyFinder = this.landManagers.get(markerType);
        if (polyFinder != null) polyFinder.clearAreasAndDeleteMarkers();
    }

    public void removeLands(MarkerType markerType, @NotNull Group group) {
        MapLandManager landManager = landManagers.get(markerType);
        if (landManager != null) landManager.removeAreasOf(group);
    }

    void loadIcons() {
        iconRegistry.getIcons().clear();
        iconRegistry.register();
    }

    @Override
    public String toString() {
        return super.toString() + '(' + api.toString() + ')';
    }

    protected void sendGlobalMessage(Player player, String message) {
        api.sendMessage(player, message);
    }

    private MapLandManager getLandManager(MarkerType markerType) {
        return landManagers.computeIfAbsent(markerType, k -> new MapLandManager(markerType, api));
    }

    protected void updateLands(MarkerType markerType, Group group,
                               Map<String, ? extends Collection<SimpleChunkLocation>> lands,
                               LandMarkerSettings settings) {
        MapLandManager polyFinder = getLandManager(markerType);
        polyFinder.removeAreasOf(group);
        polyFinder.createAreasFromWorlds(group, lands, settings, "");
    }

    protected void addLands(MarkerType markerType, Group group,
                            Map<String, ? extends Collection<SimpleChunkLocation>> lands,
                            LandMarkerSettings settings, String idSuffix) {
        MapLandManager polyFinder = getLandManager(markerType);
        polyFinder.createAreasFromWorlds(group, lands, settings, idSuffix);
    }

    public void update() {
        api.update();
    }

    private static Vector3Location center(SimpleLocation location) {
        return Vector3Location.of(
                Kingdoms.getServerX().getWorldRegistry().getWorld(location.getWorld()),
                location.getX() + 0.5, location.getY() + 0.5, location.getZ() + 0.5
        );
    }

    protected void addIconsOf(MarkerType markerType, @NotNull Land land) {
        Objects.requireNonNull(land, "land is null");
        Kingdom kingdom = land.getKingdom();
        if (kingdom == null) return;

        for (Structure structure : land.getStructures().values()) updateStructure(markerType, structure);
        if (kingdom.getHome() != null && SimpleChunkLocation.of(kingdom.getHome()).equals(land.getLocation()))
            updateHomeIcon(markerType, SimpleLocation.of(kingdom.getHome()), false);

        Nation nation = kingdom.getNation();
        if (nation != null && nation.getHome() != null && SimpleChunkLocation.of(nation.getHome()).equals(land.getLocation()))
            updateHomeIcon(markerType, SimpleLocation.of(nation.getHome()), true);
    }

    @Nullable
    public static LandMarkerSettings getIconSettings(String iconName, MessagePlaceholderProvider context) {
        YamlConfigAccessor icon = MapsConfig.ICONS.getManager().withProperty(iconName).getSection();
        Objects.requireNonNull(icon, () -> "Icon section not found for '" + iconName + '\'');
        if (icon.getBoolean("disabled")) return null;

        return translateIcons(
                new LandMarkerSettings()
                        .clickDescription(icon.getString("name"))
                        .zoom(icon.getInt("zoom", "min"), icon.getInt("zoom", "max")),
                context
        );
    }

    @Nullable
    private IconMarker updateIcon(MarkerType markerType, @NotNull SimpleLocation location,
                                  @NotNull String iconName,
                                  @Nullable MessagePlaceholderProvider context) {
        String id = iconIdOf(location);
        LandMarkerSettings iconSettings = getIconSettings(iconName, context);
        if (iconSettings == null) return null;

        return updateIcon(markerType, location, iconName, id, iconSettings);
    }

    private static LandMarkerSettings translateIcons(LandMarkerSettings iconSettings, @Nullable MessagePlaceholderProvider context) {
        if (context != null) {
            String clickDescription = iconSettings.getClickDescription();
            if (!Strings.isNullOrEmpty(clickDescription)) {
                iconSettings.clickDescription(new StaticMessenger(clickDescription).parse(context));
            }

            String hoverDescription = iconSettings.getHoverDescription();
            if (!Strings.isNullOrEmpty(hoverDescription)) {
                iconSettings.hoverDescription(new StaticMessenger(hoverDescription).parse(context));
            }
        }
        return iconSettings;
    }

    protected IconMarker updateIcon(MarkerType markerType, @NotNull SimpleLocation location,
                                    @NotNull String iconName, String id, LandMarkerSettings settings) {
        Object iconKey = iconRegistry.getIcons().get(iconName);
        if (iconKey == null) return null;

        IconMarker icon = api.updateOrAddIcon(
                markerType, id, settings,
                center(location),
                iconKey
        );
        icon.setSettings(settings);
        return icon;
    }

    protected static @NotNull String iconIdOf(SimpleLocation location) {
        return Integer.toString(location.hashCode());
    }

    void updateStructure(MarkerType markerType, @NotNull Structure structure) {
        String mainIconName = structure.getStyle().getName();
        String iconName = null;
        int level = structure.getLevel();
        Map<String, Object> icons = iconRegistry.getIcons();

        while (level > 0) {
            iconName = mainIconName + '/' + level;
            Object icon = icons.get(iconName);
            if (icon != null) break;
            iconName = null;
            level--;
        }

        if (iconName == null) {
            if (icons.containsKey(mainIconName)) {
                iconName = mainIconName;
            }
        }
        if (iconName == null) return;

        SimpleLocation location = structure.getOrigin();
        String id = iconIdOf(location);
        LandMarkerSettings iconSettings = getIconSettings(mainIconName, structure.getMessageContext());
        if (iconSettings == null) return;

        updateIcon(markerType, location, iconName, id, iconSettings);
    }

    void updateHomeIcon(MarkerType markerType, @NotNull SimpleLocation home, boolean national) {
        updateIcon(markerType, home, national ? "national-spawn" : "home", null);
    }

    protected void removeIcon(MarkerType type, String id, SimpleLocation location) {
        api.removeIconMarker(type, id, center(location));
    }

    private static final Namespace INVASION_ICON_ID = new Namespace("MapViewers", "INVASION_ICON_ID");

    public void clearInvasionAreas(Invasion invasion) {
        getLandManager(MarkerType.KINGDOMS).removeInvadedAreas(invasion);
        api.update();

        IconMarker icon = (IconMarker) invasion.getMetadata().get(INVASION_ICON_ID);
        if (icon != null) icon.delete();
    }

    public void invasionStart(MarkerType markerType, World world, Kingdom defender, Invasion invasion, Collection<SimpleChunkLocation> chunks, LandMarkerSettings settings) {
        getLandManager(MarkerType.KINGDOMS).createAreas(BukkitAdapter.adapt(world), defender, invasion, chunks, settings, "");
        Location centroid = ChunkConnections.getCentroid(invasion.getAffectedLands());
        IconMarker icon = updateIcon(markerType, SimpleLocation.of(centroid), "invasion", null);
        if (icon != null) invasion.getMetadata().put(INVASION_ICON_ID, icon);
    }
}
