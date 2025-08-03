package org.kingdoms.services.maps;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.kingdoms.abstraction.GroupOperator;
import org.kingdoms.abstraction.KingdomOperator;
import org.kingdoms.abstraction.NationOperator;
import org.kingdoms.constants.group.Group;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.group.Nation;
import org.kingdoms.constants.land.abstraction.KingdomBuildingType;
import org.kingdoms.constants.land.location.SimpleLocation;
import org.kingdoms.constants.land.structures.Structure;
import org.kingdoms.constants.land.structures.StructureType;
import org.kingdoms.constants.land.structures.type.StructureTypePowercell;
import org.kingdoms.data.Pair;
import org.kingdoms.events.general.*;
import org.kingdoms.events.general.nation.NationCreateEvent;
import org.kingdoms.events.general.nation.NationDisbandEvent;
import org.kingdoms.events.general.nation.NationSetSpawnEvent;
import org.kingdoms.events.invasion.KingdomInvadeEndEvent;
import org.kingdoms.events.invasion.KingdomInvadeEvent;
import org.kingdoms.events.items.KingdomBuildingUpgradeEvent;
import org.kingdoms.events.items.KingdomItemBreakEvent;
import org.kingdoms.events.items.KingdomItemPlaceEvent;
import org.kingdoms.events.lands.ClaimLandEvent;
import org.kingdoms.events.lands.UnclaimLandEvent;
import org.kingdoms.events.members.KingdomJoinEvent;
import org.kingdoms.events.members.KingdomLeaveEvent;
import org.kingdoms.main.KLogger;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.platform.bukkit.adapters.BukkitAdapter;
import org.kingdoms.services.maps.abstraction.markersets.MarkerListenerType;
import org.kingdoms.services.maps.abstraction.markersets.MarkerType;
import org.kingdoms.utils.internal.functional.Fn;

import java.time.Duration;
import java.util.function.Consumer;

public final class MapEventHandlers implements Listener {
    // TODO Implement this for repetitive updates
    // private static final Cache<Group, Boolean> PENDING_UPDATES = CacheHandler.newBuilder().expireAfterAccess(10, TimeUnit.SECONDS).build();

    protected MapEventHandlers() {}

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInvade(KingdomInvadeEvent event) {
        ServiceMap.invasionStart(event.getInvasion());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInvadeEnd(KingdomInvadeEndEvent event) {
        ServiceMap.clearInvasionAreas(event.getInvasion());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onClaim(ClaimLandEvent event) {
        updateKingdomLands(event);
        updateNationLands(event.getKingdom());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onNationCreate(NationCreateEvent event) {
        updateNationLands(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onKingdomCreate(KingdomCreateEvent event) {
        updateKingdomLands(event);
    }

    static void updateKingdomLands(KingdomOperator kingdom) {
        // We delay it a second so the event can change the data.
        Kingdoms.taskScheduler().async().delayed(Duration.ofSeconds(1), () -> ServiceMap.updateLands(kingdom.getKingdom()));
    }

    static void updateNationLands(NationOperator nationOperator) {
        if (nationOperator == null) return;

        Nation nation = nationOperator.getNation();
        if (nation == null) return;

        Kingdoms.taskScheduler().async().delayed(Duration.ofSeconds(1), () -> ServiceMap.updateLands(nation));
    }

    static void updateGroup(GroupOperator groupOperator) {
        Group group = groupOperator.getGroup();
        if (group instanceof Kingdom) updateKingdomLands((KingdomOperator) group);
        else updateNationLands((NationOperator) group);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onUnclaim(UnclaimLandEvent event) {
        updateKingdomLands(event);
        updateNationLands(event.getKingdom().getNation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onRename(GroupRenameEvent event) {
        updateGroup(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTagRename(GroupRenameTagEvent event) {
        updateGroup(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLoreChange(KingdomLoreChangeEvent event) {
        updateGroup(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onJoinEvent(KingdomJoinEvent event) {
        updateGroup(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLeaveEvent(KingdomLeaveEvent event) {
        updateGroup(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onRelationshipChange(GroupRelationshipChangeEvent event) {
        updateGroup(event.getFirst());
        updateGroup(event.getSecond());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onFlagChange(GroupBannerChangeEvent event) {
        if (event.isBanner() && event.getGroup().getFlag() != null) return;
        updateGroup(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onColorChange(GroupColorChangeEvent event) {
        updateGroup(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHomeChange(KingdomSetHomeEvent event) {
        changeHome(event, event.getKingdom());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHomeChange(NationSetSpawnEvent event) {
        changeHome(event, event.getNation());
    }

    private static void powercell(Consumer<MarkerType> powercell) {
        if (!MarkerType.POWERCELL.isEnabled()) return;
        powercell.accept(MarkerType.POWERCELL);
    }

    private static void iconEvent(MarkerListenerType listenerType, Group group, Consumer<MarkerType> handler) {
        for (Pair<MarkerType, MarkerListenerType.IconContainer> markerType : MarkerType.<MarkerListenerType.IconContainer>getMarkerTypes(listenerType)) {
            if (!markerType.getValue().showIcons()) continue;
            if (!markerType.getKey().isEnabled()) continue;
            if (group != null && (markerType.getKey() instanceof MarkerListenerType.Ignorable<?>)) {
                if (((MarkerListenerType.Ignorable<?>) markerType.getKey()).isIgnored(Fn.cast(group))) continue;
            }
            handler.accept(markerType.getKey());
        }
    }

    private static void changeHome(LocationChangeEvent event, Group group) {
        MarkerListenerType markerListenerType = event instanceof KingdomSetHomeEvent ? MarkerListenerType.KINGDOMS : MarkerListenerType.NATIONS;
        if (event.getOldLocation() != null)
            iconEvent(markerListenerType, group, markerType -> ServiceMap.removeHomeIcon(markerType, event.getOldLocation()));
        if (event.getNewLocation() != null)
            iconEvent(markerListenerType, group, markerType -> ServiceMap.updateHomeIcon(markerType, event.getNewLocation(), false));

        ServiceMap.update();
    }


    @SuppressWarnings("ConstantConditions")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onStructurePlace(KingdomItemPlaceEvent<Structure> event) {
        if (!(event.getKingdomBlock() instanceof Structure)) return;
        iconEvent(MarkerListenerType.KINGDOMS, event.getKingdomBlock().getLand().getKingdom(), markerType -> ServiceMap.updateStructure(markerType, event.getKingdomBlock()));

        if (event.getKingdomBlock().getStyle().getType() instanceof StructureTypePowercell) {
            powercell(pc -> {
                ServiceMap.updatePowercellMarkers(event.getKingdomBlock(), event.getKingdomBlock().getLevel());
                ServiceMap.updateStructure(pc, event.getKingdomBlock());
            });
        }

        ServiceMap.update();
    }

    @SuppressWarnings("ConstantConditions")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onStructureBreak(KingdomItemBreakEvent<Structure> event) {
        if (!(event.getKingdomBlock() instanceof Structure)) return;
        Structure structure = event.getKingdomBlock();
        iconEvent(
                structure.getStyle().getType().isNationalNexus() ? MarkerListenerType.NATIONS : MarkerListenerType.KINGDOMS,
                structure.getLand().getKingdom(),
                markerType -> ServiceMap.removeStructureIcon(markerType, structure)
        );

        if (structure.getStyle().getType() instanceof StructureTypePowercell) {
            ServiceMap.removePowercellMarkers(structure);
            powercell(pc -> ServiceMap.removeStructureIcon(pc, structure));
        }
        ServiceMap.update();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onStructureUpgrade(KingdomBuildingUpgradeEvent event) {
        KingdomBuildingType<?, ?, ?> type = event.getBuilding().getStyle().getType();
        if (!(type instanceof StructureType)) return;
        Structure structure = (Structure) event.getBuilding();

        iconEvent(
                ((StructureType) type).isNationalNexus() ? MarkerListenerType.NATIONS : MarkerListenerType.KINGDOMS,
                structure.getLand().getKingdom(),
                markerType -> ServiceMap.updateStructure(markerType, structure)
        );

        if (type instanceof StructureTypePowercell) {
            powercell(pc -> {
                ServiceMap.clearLands(pc, event.getBuilding().getLand().getKingdom());
                ServiceMap.updatePowercellMarkers(structure, event.getNewLevel());
                ServiceMap.updateStructure(pc, structure);
            });
        }
        ServiceMap.update();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onGroupHiddenState(GroupHiddenStateChangeEvent event) {
        Group group = event.getGroup();
        if (event.getNewHiddenState()) {
            if (group instanceof Kingdom) removeKingdom((Kingdom) group);
            else removeNation((Nation) group);
        } else {
            updateGroup(group);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onKingdomDisband(KingdomDisbandEvent event) {
        removeKingdom(event.getKingdom());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onNationDisband(NationDisbandEvent event) {
        removeNation(event.getNation());
    }

    private static void removeKingdom(Kingdom kingdom) {
        for (Pair<MarkerType, MarkerListenerType.Kingdoms> markerType : MarkerType.<MarkerListenerType.Kingdoms>getMarkerTypes(MarkerListenerType.KINGDOMS)) {
            if (markerType.getValue().showLands && !markerType.getValue().isIgnored(kingdom))
                ServiceMap.clearLands(markerType.getKey(), kingdom);
        }
        if (kingdom.getHome() != null) {
            // Structures are removed by silent unclaim
            iconEvent(MarkerListenerType.KINGDOMS, kingdom, markerType ->
                    ServiceMap.removeHomeIcon(markerType, kingdom.getHome()));
        }

        powercell(pc -> ServiceMap.clearLands(pc, kingdom));
        ServiceMap.update();
    }

    private static void removeNation(Nation nation) {
        for (Pair<MarkerType, MarkerListenerType.Nations> markerType : MarkerType.<MarkerListenerType.Nations>getMarkerTypes(MarkerListenerType.NATIONS)) {
            if (markerType.getValue().showLands) ServiceMap.clearLands(markerType.getKey(), nation);
        }
        if (nation.getNexus() != null) {
            iconEvent(MarkerListenerType.NATIONS, nation, markerType -> ServiceMap.removeIcon(markerType, nation.getNexus()));
        }
        if (nation.getHome() != null) {
            iconEvent(MarkerListenerType.NATIONS, nation, markerType -> ServiceMap.removeHomeIcon(markerType, nation.getHome()));
        }
        ServiceMap.update();
    }
}
