package org.kingdoms.enginehub;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.land.location.SimpleChunkLocation;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.permissions.KingdomsDefaultPluginPermission;
import org.kingdoms.platform.bukkit.channel.BlockMarker;
import org.kingdoms.platform.bukkit.channel.PluginChannels;
import org.kingdoms.services.Service;

import java.awt.*;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class ServiceWorldEditSessionProtection implements Service {
    @Override
    public void enable() {
        // https://worldedit.enginehub.org/en/latest/api/concepts/edit-sessions/
        WorldEdit.getInstance().getEventBus().register(this);
    }

    @Override
    public void disable() {
        WorldEdit.getInstance().getEventBus().unregister(this);
    }

    private static final class KingdomsProtectionExtent extends AbstractDelegateExtent {
        private final Set<org.kingdoms.server.location.BlockVector3> excluded = new HashSet<>();
        private final Player actor;
        private final World world;
        private final Kingdom kingdom;
        private boolean beforeChange = true;

        protected KingdomsProtectionExtent(Extent extent, Player actor, World world, Kingdom kingdom) {
            super(extent);
            this.actor = actor;
            this.world = world;
            this.kingdom = kingdom;
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
            Location bukkitLocation = BukkitAdapter.adapt(world, location);
            SimpleChunkLocation chunk = SimpleChunkLocation.of(bukkitLocation);
            if (!kingdom.isClaimed(chunk)) {
                excluded.add(org.kingdoms.platform.bukkit.adapters.BukkitAdapter.adapt(bukkitLocation).toBlockVector());
                return false;
            }

            return super.setBlock(location, block);
        }

        @Override
        protected Operation commitBefore() {
            // This is called twice, once before calling any setBlock() and once
            // after all the setBlock() calls have been made.

            if (beforeChange) {
                beforeChange = false;
            } else {
                if (!excluded.isEmpty()) {
                    EngineHubLang.WORLDEDIT_EXCLUDED.sendError(actor, "blocks", excluded.size());
                    if (PluginChannels.isSupported()) {
                        PluginChannels.sendBlockMarker(actor, excluded,
                                new BlockMarker(Duration.ofSeconds(10), Color.RED, ""));
                    }
                }
            }

            return super.commitBefore();
        }
    }

    @Subscribe
    public void onEdit(EditSessionEvent event) {
        if (event.getStage() != EditSession.Stage.BEFORE_HISTORY) return;

        Actor editor = event.getActor();
        if (editor == null || !editor.isPlayer()) return;

        Player player = Bukkit.getPlayer(editor.getUniqueId());
        Objects.requireNonNull(player, () -> "Actor " + editor + " for WorldEdit is a null player");
        if (KingdomsDefaultPluginPermission.WORLDEDIT_BYPASS_EDIT$PROTECTION.hasPermission(player, false)) return;

        KingdomPlayer kp = KingdomPlayer.getKingdomPlayer(player);
        if (!kp.hasKingdom() || kp.isAdmin()) return;

        Kingdom kingdom = kp.getKingdom();
        World world = BukkitAdapter.asBukkitWorld(event.getWorld()).getWorld();

        // This won't work here because extent isn't executed immediatelly.
        event.setExtent(new KingdomsProtectionExtent(event.getExtent(), player, world, kingdom));
    }
}
