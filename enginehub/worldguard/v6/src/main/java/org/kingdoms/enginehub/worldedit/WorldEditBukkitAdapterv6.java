package org.kingdoms.enginehub.worldedit;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.LegacyBlockRegistry;
import com.sk89q.worldedit.world.registry.LegacyWorldData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.server.location.BlockVector2;
import org.kingdoms.server.location.BlockVector3;
import org.kingdoms.server.location.Vector3;

class WorldEditBukkitAdapterv6 implements XWorldEditBukkitAdapter {
    private static final WorldEditPlugin INSTANCE;

    static {
        INSTANCE = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
    }

    public BukkitPlayer adapt(Player player) {
        return INSTANCE.wrapPlayer(player);
    }

    @Override
    public BukkitWorld asBukkitWorld(com.sk89q.worldedit.world.World world) {
        return (BukkitWorld) world;
    }

    @Override
    public EditSession createEditSession(World world) {
        return WorldEdit.getInstance().getEditSessionFactory().getEditSession(adapt(world), -1);
    }

    @Override
    public com.sk89q.worldedit.world.World adapt(World world) {
        // https://github.com/EngineHub/WorldEdit/blob/fa8dbe2aa3e812428899ef91e8bcdfed0f3e87b7/worldedit-bukkit/src/main/java/com/sk89q/worldedit/bukkit/BukkitAdapter.java#L124-L127
        return new BukkitWorld(world);
    }

    @Override
    @NotNull
    public BlockVector3 adaptBlockVector(@NotNull Object blockVector) {
        Vector vector = (Vector) blockVector;
        return adaptBlockVector(vector);
    }

    @Override
    @NotNull
    public XClipboard adapt(@NotNull Clipboard clipboard) {
        return new Clipboardv6(clipboard);
    }

    @Override
    @NotNull
    public XRegion adapt(@NotNull Region region) {
        return new Regionv6(region);
    }

    @Override
    public Location adapt(World world, Object vector) {
        return BukkitUtil.toLocation(world, (Vector) vector);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Object blockData(Object block) {
        return BukkitUtil.toBlock((BlockWorldVector) block);
    }

    @Override
    public void setClipboard(@NotNull LocalSession session, @NotNull Clipboard clipboard) {
        session.setClipboard(new ClipboardHolder(clipboard, LegacyWorldData.getInstance()));
    }

    @Override
    @NotNull
    public CuboidRegion createCuboidRegion(@NotNull org.kingdoms.server.location.BlockVector3 min, @NotNull org.kingdoms.server.location.BlockVector3 max) {
        return new CuboidRegion(adaptToVector(min), adaptToVector(max));
    }

    @Override
    @NotNull
    public BlockTransformExtent createBlockTransformExtent(@NotNull Extent extent, @NotNull Transform transform) {
        return new BlockTransformExtent(extent, transform, new LegacyBlockRegistry());
    }

    @Override
    @NotNull
    public ForwardExtentCopy createForwardExtentCopy(@NotNull Extent source, @NotNull Region region,
                                                     @NotNull BlockVector3 from, @NotNull Extent destination,
                                                     @NotNull BlockVector3 to) {
        return new ForwardExtentCopy(source, region, adaptToVector(from), destination, adaptToVector(to));
    }

    @Override
    public Vector3 applyTransformation(@NotNull Transform transform, @NotNull Vector3 input) {
        return adapt(transform.apply(adapt(input)));
    }

    protected static Vector adaptToVector(org.kingdoms.server.location.BlockVector3 vector) {
        return new Vector(vector.getX(), vector.getY(), vector.getZ());
    }

    protected static Vector adapt(Vector3 vector) {
        return new Vector(vector.getX(), vector.getY(), vector.getZ());
    }

    protected static Vector3 adapt(Vector vector) {
        return Vector3.of(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    protected static BlockVector3 adaptBlockVector(Vector vector) {
        return BlockVector3.of(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    protected static BlockVector2 adapt(Vector2D vector) {
        return BlockVector2.of(vector.getBlockX(), vector.getBlockZ());
    }
}
