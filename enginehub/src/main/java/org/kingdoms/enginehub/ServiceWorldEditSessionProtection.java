package org.kingdoms.enginehub;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.land.location.SimpleChunkLocation;
import org.kingdoms.constants.land.location.SimpleLocation;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.locale.KingdomsLang;
import org.kingdoms.permissions.KingdomsDefaultPluginPermission;
import org.kingdoms.services.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class ServiceWorldEditSessionProtection implements Service {
    @Override
    public void enable() {
        // https://worldedit.enginehub.org/en/latest/api/concepts/edit-sessions/
        WorldEdit.getInstance().getEventBus().register(this);
    }

    private static final class KingdomsProtectionExtent extends AbstractDelegateExtent {
        private final Function<BlockVector3, Boolean> filterFunction;

        protected KingdomsProtectionExtent(Extent extent, Function<BlockVector3, Boolean> excludeFunction) {
            super(extent);
            this.filterFunction = excludeFunction;
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
            if (!filterFunction.apply(location)) return false;
            return super.setBlock(location, block);
        }
    }

    private static void paste0(File file, SimpleLocation location) throws IOException, WorldEditException {
        // https://worldedit.enginehub.org/en/latest/api/examples/clipboard/
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        try (ClipboardReader reader = format.getReader(Files.newInputStream(file.toPath()))) {
            Clipboard clipboard = reader.read();

            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(location.getBukkitWorld());
            // For newer versions WorldEdit.getInstance().newEditSession(world)
            try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1)) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(operation);
            }
        }
    }

    @Subscribe
    public void onEdit(EditSessionEvent event) {
        Actor editor = event.getActor();
        if (editor == null || !editor.isPlayer()) return;

        Player player = Bukkit.getPlayer(editor.getUniqueId());
        Objects.requireNonNull(player, () -> "Actor " + editor + " for WorldEdit is a null player");
        if (KingdomsDefaultPluginPermission.WORLD$EDIT_BYPASS_EDIT$PROTECTION.hasPermission(player, false)) return;

        KingdomPlayer kp = KingdomPlayer.getKingdomPlayer(player);
        if (!kp.hasKingdom() || kp.isAdmin()) return;

        Kingdom kingdom = kp.getKingdom();
        World world = BukkitAdapter.asBukkitWorld(event.getWorld()).getWorld();
        AtomicInteger excluded = new AtomicInteger();

        event.setExtent(new KingdomsProtectionExtent(event.getExtent(), (touchedLocation) -> {
            Location bukkitLocation = BukkitAdapter.adapt(world, touchedLocation);
            SimpleChunkLocation chunk = SimpleChunkLocation.of(bukkitLocation);
            if (!kingdom.isClaimed(chunk)) {
                excluded.incrementAndGet();
                return false;
            }
            return true;
        }));

        if (excluded.get() > 0) {
            KingdomsLang.WORLD_EDIT_EXCLUDED.sendError(player, "blocks", excluded.get());
        }
    }
}
