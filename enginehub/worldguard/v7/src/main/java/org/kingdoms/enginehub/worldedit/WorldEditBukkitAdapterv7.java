package org.kingdoms.enginehub.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

class WorldEditBukkitAdapterv7 implements XWorldEditBukkitAdapter {
    public BukkitPlayer adapt(Player player) {
        return BukkitAdapter.adapt(player);
    }

    @Override
    public BukkitWorld asBukkitWorld(com.sk89q.worldedit.world.World world) {
        return BukkitAdapter.asBukkitWorld(world);
    }

    @Override
    public EditSession createEditSession(World world) {
        return WorldEdit.getInstance().newEditSession(adapt(world));
    }

    @Override
    public com.sk89q.worldedit.world.World adapt(World world) {
        return BukkitAdapter.adapt(world);
    }

    @Override
    public Location adapt(World world, Object vector) {
        return BukkitAdapter.adapt(world, (BlockVector3) vector);
    }

    @Override
    public org.kingdoms.server.location.BlockVector3 adaptBlockVector(@NotNull Object blockVector) {
        BlockVector3 vector3 = (BlockVector3) blockVector;
        return adapt(vector3);
    }

    @Override
    @NotNull
    public XClipboard adapt(@NotNull Clipboard clipboard) {
        return new Clipboardv7(clipboard);
    }

    @Override
    @NotNull
    public XRegion adapt(@NotNull Region region) {
        return new Regionv7(region);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object blockData(Object block) {
        return BukkitAdapter.adapt((BlockStateHolder) block);
    }

    @Override
    public void setClipboard(@NotNull LocalSession session, @NotNull Clipboard clipboard) {
        session.setClipboard(new ClipboardHolder(clipboard));
    }

    @Override
    @NotNull
    public CuboidRegion createCuboidRegion(@NotNull org.kingdoms.server.location.BlockVector3 min, @NotNull org.kingdoms.server.location.BlockVector3 max) {
        return new CuboidRegion(adapt(min), adapt(max));
    }

    @Override
    @NotNull
    public BlockTransformExtent createBlockTransformExtent(@NotNull Extent extent, @NotNull Transform transform) {
        return new BlockTransformExtent(extent, transform);
    }

    @Override
    @NotNull
    public ForwardExtentCopy createForwardExtentCopy(@NotNull Extent source, @NotNull Region region,
                                                     @NotNull org.kingdoms.server.location.BlockVector3 from, @NotNull Extent destination,
                                                     @NotNull org.kingdoms.server.location.BlockVector3 to) {
        return new ForwardExtentCopy(source, region, adapt(from), destination, adapt(to));
    }

    @Override
    public org.kingdoms.server.location.Vector3 applyTransformation(@NotNull Transform transform, org.kingdoms.server.location.Vector3 input) {
        return adapt(transform.apply(adapt(input)));
    }

    protected static BlockVector3 adapt(org.kingdoms.server.location.BlockVector3 vector) {
        return BlockVector3.at(vector.getX(), vector.getY(), vector.getZ());
    }

    protected static org.kingdoms.server.location.BlockVector3 adapt(BlockVector3 vector) {
        return org.kingdoms.server.location.BlockVector3.of(vector.getX(), vector.getY(), vector.getZ());
    }

    protected static org.kingdoms.server.location.Vector3 adapt(Vector3 vector) {
        return org.kingdoms.server.location.Vector3.of(vector.getX(), vector.getY(), vector.getZ());
    }

    protected static Vector3 adapt(org.kingdoms.server.location.Vector3 vector) {
        return Vector3.at(vector.getX(), vector.getY(), vector.getZ());
    }

    protected static org.kingdoms.server.location.BlockVector2 adapt(BlockVector2 vector) {
        return org.kingdoms.server.location.BlockVector2.of(vector.getX(), vector.getZ());
    }
}
