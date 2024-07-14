package org.kingdoms.enginehub.schematic.blocks;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.block.ExtentBlockCopy;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.Identity;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.world.World;
import org.kingdoms.enginehub.WorldEditAdapter;
import org.kingdoms.platform.bukkit.adapters.BukkitAdapter;
import org.kingdoms.server.location.BlockLocation3;

import java.io.Closeable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator which will paste blocks one by one.
 * Clipboard -> ClipboardHolder -> createPaste() -> PasteBuilder -> build() -> ForwardExtentCopy -> ExtentBlockCopy
 * https://github.com/EngineHub/WorldEdit/blob/29df0f2847d6b16ea9d6d76a28e01810d306ff6e/worldedit-core/src/main/java/com/sk89q/worldedit/command/ClipboardCommands.java#L175-L187
 */
public final class ClipboardBlockCopyIterator implements Iterator<FunctionalWorldEditExtentBlock>, Closeable {
    public static final Transform IDENTITY_TRANSFORM = new Identity();

    private final Clipboard clipboard;
    private final Mask mask;
    private final RegionFunction blockCopy;
    private final ClipboardBlockIterator iterator;
    private final EditSession destination;
    BlockLocation3 origin;
    private final boolean changeInstantly;

    private Boolean cachedHasNext;
    private WorldEditExtentBlock lastNext;

    public ClipboardBlockCopyIterator(Clipboard clipboard, BlockLocation3 origin /* aka. to/destination */,
                                      Comparator<org.kingdoms.server.location.BlockVector3> sortingStrategy,
                                      boolean ignoreAirBlocks, boolean changeInstantly) {
        this.clipboard = clipboard;
        this.origin = origin;
        this.changeInstantly = changeInstantly;
        this.iterator = new ClipboardBlockIterator(origin.toVector(), this.clipboard, sortingStrategy);
        this.mask = ignoreAirBlocks ? new ExistingBlockMask(this.clipboard) : Masks.alwaysTrue();

        World world = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(BukkitAdapter.adapt(origin.getWorld()));
        this.destination = WorldEdit.getInstance().newEditSession(world);

        // public ForwardExtentCopy(Extent source, Region region, BlockVector3 from, Extent destination, BlockVector3 to)
        // new ForwardExtentCopy(extent, this.clipboard.getRegion(), this.clipboard.getOrigin(), this.targetExtent, this.to);
        ExtentBlockCopy normalCopy = new ExtentBlockCopy(
                // Source
                this.clipboard, this.clipboard.getOrigin(),

                // Destination
                this.destination, WorldEditAdapter.adapt(origin),

                // Transformation of source blocks
                IDENTITY_TRANSFORM
        );

        this.blockCopy = changeInstantly ? new InstantBlockCopy(normalCopy, this.destination) : normalCopy;
    }

    @Override
    public void close() {
        // Since we're completing the operation on every iteration,
        // this is not really needed, but it's here for the sake of semantics and compatibility.
        this.cachedHasNext = false;
        this.destination.close();
    }

    private boolean skipMasked() {
        while (iterator.hasNext()) {
            WorldEditExtentBlock next = iterator.next();
            BlockVector3 pos = WorldEditAdapter.adapt(next.getRelativePosition());

            if (mask.test(pos)) {
                lastNext = next;
                return true;
            }
        }

        close();
        return false;
    }

    private static final class InstantBlockCopy implements RegionFunction {
        private final RegionFunction main;
        private final EditSession editSession;

        private InstantBlockCopy(RegionFunction main, EditSession editSession) {
            this.main = main;
            this.editSession = editSession;
        }

        @Override
        public boolean apply(BlockVector3 position) throws WorldEditException {
            boolean applied = main.apply(position);
            Operations.completeBlindly(editSession.commit());
            return applied;
        }
    }

    @Override
    public boolean hasNext() {
        if (cachedHasNext != null) return cachedHasNext;
        return cachedHasNext = skipMasked();
    }

    @Override
    public FunctionalWorldEditExtentBlock next() {
        if (!hasNext()) throw new NoSuchElementException("No more blocks to iterate");
        cachedHasNext = null;
        WorldEditExtentBlock next = lastNext;

        return new FunctionalWorldEditExtentBlock(
                next.getExtent(),
                next.getRelativePosition(), next.getAbsolutePosition(),
                this.blockCopy
        );
    }
}
