package org.kingdoms.enginehub;

// import org.bukkit.Material;
// import org.jetbrains.annotations.Nullable;
// import org.kingdoms.nms.v1_20.BlockFactory;
// import org.kingdoms.properties.Property;
// import org.kingdoms.schematic.*;
//
// import java.util.*;

// public class BukkitBlockRegistry extends BundledBlockRegistry {
//     private final BlockFactory NMS = new BlockFactory();
//     private final Map<Material, BukkitBlockMaterial> materialMap = new EnumMap<>(Material.class);
//
//     private static Material adapt(BlockType blockType) {
//         Objects.requireNonNull(blockType);
//         if (!blockType.getId().startsWith("minecraft:")) {
//             throw new IllegalArgumentException("Bukkit only supports Minecraft blocks");
//         } else {
//             return Material.getMaterial(blockType.getId().substring(10).toUpperCase(Locale.ROOT));
//         }
//     }
//
//     @Override
//     public BlockMaterial getMaterial(BlockType blockType) {
//         Material mat = adapt(blockType);
//         if (mat == null) {
//             return null;
//         }
//         return materialMap.computeIfAbsent(mat, material -> new BukkitBlockMaterial(BukkitBlockRegistry.super.getMaterial(blockType), material));
//     }
//
//     @SuppressWarnings("unchecked") @Nullable
//     @Override
//     public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
//         return (Map<String, ? extends Property<?>>) NMS.getProperties(blockType.getId());
//     }
//
//     @Override
//     public OptionalInt getInternalBlockStateId(BlockState state) {
//         BlockType blockType = state.getBlockType();
//         return NMS.getInternalBlockStateId(blockType.getId(), state.getStates(), blockType == BlockTypes.AIR);
//     }
//
//     public static class BukkitBlockMaterial extends PassthroughBlockMaterial {
//
//         private final Material material;
//
//         public BukkitBlockMaterial(@Nullable BlockMaterial material, Material bukkitMaterial) {
//             super(material);
//             this.material = bukkitMaterial;
//         }
//
//         @Override
//         public boolean isAir() {
//             switch (material) {
//                 case AIR:
//                 case CAVE_AIR:
//                 case VOID_AIR:
//                     return true;
//                 default:
//                     return false;
//             }
//         }
//
//         @Override
//         public boolean isSolid() {
//             return material.isSolid();
//         }
//
//         @Override
//         public boolean isBurnable() {
//             return material.isBurnable();
//         }
//
//         @SuppressWarnings("deprecation")
//         @Override
//         public boolean isTranslucent() {
//             return material.isTransparent();
//         }
//     }
// }
