package org.kingdoms.platform.bukkit.item;

import com.cryptomorin.xseries.reflection.ReflectiveNamespace;
import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftMapping;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftPackage;
import org.bukkit.inventory.ItemStack;
import org.kingdoms.nbt.tag.NBTTagType;
import org.kingdoms.platform.bukkit.adapters.BukkitAdapter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Objects;

import static com.cryptomorin.xseries.reflection.XReflection.ofMinecraft;
import static com.cryptomorin.xseries.reflection.XReflection.supports;

public final class ItemNBT {
    public static final boolean CAN_ACCESS_UNBREAKABLE = supports(11), SUPPORTS_COMPONENTS;
    public static final MethodHandle AS_NMS_COPY;
    public static final MethodHandle AS_BUKKIT_COPY;
    public static final MethodHandle SET_TAG, CUSTOM_DATA_CTOR;
    public static final MethodHandle GET_TAG, COPY_TAG;
    public static final Object CUSTOM_DATA_TYPE;

    static {
        MethodHandle asNmsCopy = null;
        MethodHandle asBukkitCopy = null;
        MethodHandle setTag, customDataCtor = null;
        MethodHandle getTag, copyTag = null;
        Object customDataType = null;
        boolean supportsComponents = false;

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        ReflectiveNamespace ns = XReflection.namespaced();
        Class<?> crafItemStack = XReflection.ofMinecraft().inPackage(MinecraftPackage.CB, "inventory").named("CraftItemStack").unreflect();
        Class<?> nmsItemStack = XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.item").named("ItemStack").unreflect();
        Class<?> CompoundTagClass = ofMinecraft()
                .inPackage(MinecraftPackage.NMS, "nbt")
                .map(MinecraftMapping.MOJANG, "CompoundTag")
                .map(MinecraftMapping.SPIGOT, "NBTTagCompound")
                .unreflect();
        // Why does this show up as "CompoundTag" in stacktraces???!?!??

        try {
            asNmsCopy = lookup.findStatic(crafItemStack, "asNMSCopy", MethodType.methodType(nmsItemStack, ItemStack.class));
            asBukkitCopy = lookup.findStatic(crafItemStack, "asBukkitCopy", MethodType.methodType(ItemStack.class, nmsItemStack));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        try {
            // 1.20.5 "components"
            Class<?> DataComponentsClass = XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "core.component").named("DataComponents").reflect();
            Class<?> DataComponentHolderClass = XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "core.component").named("DataComponentHolder").reflect();
            Class<?> DataComponentTypeClass = XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "core.component").named("DataComponentType").reflect();
            Class<?> CustomDataClass = XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.item.component").named("CustomData").reflect();

            /*
             * @Nullable
             * public <T> T b(DataComponentType<? super T> datacomponenttype, @Nullable T t0) {
             *      return this.r.b(datacomponenttype, t0);
             * }
             */
            setTag = XReflection.of(nmsItemStack).method()
                    .map(MinecraftMapping.MOJANG, "set")
                    .map(MinecraftMapping.OBFUSCATED, "b")
                    .returns(Object.class)
                    .parameters(DataComponentTypeClass, Object.class)
                    .reflect();

            /*
             * @Nullable
             * default <T> T a(DataComponentType<? extends T> var0) {
             *      return this.a().a(var0);
             * }
             */
            getTag = XReflection.of(nmsItemStack).method()
                    .map(MinecraftMapping.MOJANG, "get")
                    .map(MinecraftMapping.OBFUSCATED, "a")
                    .returns(Object.class)
                    .parameters(DataComponentTypeClass)
                    .reflect();

            /*
             * public NBTTagCompound c() {
             *      return this.e.i();
             * }
             */
            copyTag = XReflection.of(CustomDataClass).method()
                    .map(MinecraftMapping.MOJANG, "copyTag")
                    .map(MinecraftMapping.OBFUSCATED, XReflection.v(21, 4, "d").orElse("c"))
                    .returns(CompoundTagClass)
                    .reflect();

            /*
             * private CustomData(NBTTagCompound var0) {
             *     this.e = var0;
             * }
             */
            Constructor<?> customDataCtorJvm = CustomDataClass.getDeclaredConstructor(CompoundTagClass);
            customDataCtorJvm.setAccessible(true);
            customDataCtor = lookup.unreflectConstructor(customDataCtorJvm);

            // net.minecraft.core.component.DataComponents#CUSTOM_DATA
            customDataType = XReflection.of(DataComponentsClass).field().asStatic().getter()
                    .returns(DataComponentTypeClass)
                    .map(MinecraftMapping.MOJANG, "CUSTOM_DATA")
                    .map(MinecraftMapping.OBFUSCATED, "b")
                    .reflect().invoke();

            supportsComponents = true;
        } catch (Throwable ex) {
            try {
                setTag = lookup.findVirtual(nmsItemStack,
                        XReflection.v(18, "c").orElse("setTag"), MethodType.methodType(void.class, CompoundTagClass));

                getTag = lookup.findVirtual(nmsItemStack,
                        XReflection.v(19, "v").v(18, "t").orElse("getTag"), MethodType.methodType(CompoundTagClass));
            } catch (NoSuchMethodException | IllegalAccessException ex2) {
                RuntimeException newEx = new RuntimeException(ex2);
                newEx.addSuppressed(ex);
                throw newEx;
            }
        }

        AS_NMS_COPY = asNmsCopy;
        AS_BUKKIT_COPY = asBukkitCopy;
        SET_TAG = setTag;
        GET_TAG = getTag;
        COPY_TAG = copyTag;
        CUSTOM_DATA_TYPE = customDataType;
        CUSTOM_DATA_CTOR = customDataCtor;
        SUPPORTS_COMPONENTS = supportsComponents;
    }

    private ItemNBT() {
    }

    private static Object asNMSCopy(ItemStack item) {
        try {
            return AS_NMS_COPY.invoke(item);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    /**
     * Only pass a NMS Itemstack!
     *
     * @param nmsItem The NMS item to convert
     * @return The converted Item
     */
    private static ItemStack asBukkitCopy(Object nmsItem) {
        try {
            return (ItemStack) AS_BUKKIT_COPY.invoke(nmsItem);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    /**
     * Sets the NBT tag of an item
     *
     * @param tag  The new tag
     * @param item The ItemStack
     * @return The modified itemStack
     */
    public static ItemStack setTag(ItemStack item, org.kingdoms.nbt.tag.NBTTagCompound tag) {
        Object nbtTag = BukkitAdapter.adapt(tag);
        Object nmsItem = Objects.requireNonNull(asNMSCopy(item), () -> "NMS copy is null for " + item);

        try {
            if (SUPPORTS_COMPONENTS) {
                Object customData = CUSTOM_DATA_CTOR.invoke(nbtTag);
                SET_TAG.invoke(nmsItem, CUSTOM_DATA_TYPE, customData);
            } else {
                SET_TAG.invoke(nmsItem, nbtTag);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return asBukkitCopy(nmsItem);
    }

    /**
     * Gets the NBTTag of an item. In case of any error it returns a blank one.
     *
     * @param item The ItemStack to get the tag for
     * @return The NBTTagCompound of the ItemStack or a new one if it had none or an error occurred
     */
    public static org.kingdoms.nbt.tag.NBTTagCompound getTag(ItemStack item) {
        Objects.requireNonNull(item, "Cannot get tag of null item");
        Object nmsItem = asNMSCopy(item);

        // Happens in older versions for items like AIR
        if (nmsItem == null) return org.kingdoms.nbt.tag.NBTTagCompound.empty();

        Object tag = null;
        try {
            if (SUPPORTS_COMPONENTS) {
                tag = GET_TAG.invoke(nmsItem, CUSTOM_DATA_TYPE);
                if (tag != null) tag = COPY_TAG.invoke(tag);
            } else {
                tag = GET_TAG.invoke(nmsItem);
            }
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
        if (tag == null) return org.kingdoms.nbt.tag.NBTTagCompound.empty();

        return BukkitAdapter.adapt(NBTTagType.COMPOUND, tag);
    }

    // public static ItemStack setUnbreakable(ItemStack item, boolean unbreakable) {
    //     if (CAN_ACCESS_UNBREAKABLE) {
    //         ItemMeta meta = item.getItemMeta();
    //         meta.setUnbreakable(unbreakable);
    //         item.setItemMeta(meta);
    //         return item;
    //     }
    //
    //     NBTTagCompound tag = getTag(item);
    //     tag.set("Unbreakable", NBTTagType.BOOLEAN, unbreakable);
    //     return setTag(item, tag);
    // }

    /**
     * https://minecraft.fandom.com/wiki/Player.dat_format#General_Tags
     * Doesn't support the full minecraft:block_predicate format.
     */
    // public static void setCanPlaceOnAndDestroy(NBTTagCompound tag, Collection<XMaterial> canPlaceOn, Collection<XMaterial> canDestroy) {
    //     NBTWrappers.NBTTagList<String> canPlaceOnNBT = new NBTWrappers.NBTTagList<>();
    //     for (XMaterial material : canPlaceOn) {
    //         if (!material.isSupported()) continue;
    //         canPlaceOnNBT.add(new NBTWrappers.NBTTagString("minecraft:" + material.parseMaterial().name().toLowerCase(Locale.ENGLISH)));
    //     }
    //
    //     NBTWrappers.NBTTagList<String> canDestroyNBT = new NBTWrappers.NBTTagList<>();
    //     for (XMaterial material : canDestroy) {
    //         if (!material.isSupported()) continue;
    //         canDestroyNBT.add(new NBTWrappers.NBTTagString("minecraft:" + material.parseMaterial().name().toLowerCase(Locale.ENGLISH)));
    //     }
    //
    //     tag.set("CanDestroy", canDestroyNBT);
    //     tag.set("CanPlaceOn", canPlaceOnNBT);
    // }

//    protected static ItemStack setAttributes(ItemStack item, boolean unbreakable) {
//        if (XReflection.supports(9)) {
//            ItemMeta meta = item.getItemMeta();
//            meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new AttributeModifier("34", 343, AttributeModifier.Operation.ADD_NUMBER));
//            item.setItemMeta(meta);
//            return item;
//        }
//
//        NBTTagCompound tag = getTag(item);
//        NBTWrappers.NBTTagList<NBTTagCompound> modifiers = new NBTWrappers.NBTTagList<>();
//        NBTTagCompound attribute = new NBTTagCompound();
//
//        // https://minecraft.gamepedia.com/Attribute
//        UUID id = UUID.randomUUID();
//        attribute.set("AttributeName", NBTTagType.STRING, "generic.attackSpeed");
//        attribute.set("Name", NBTTagType.STRING, "generic.attackSpeed");
//        attribute.set("Amount", NBTTagType.INT, 34);
//        attribute.set("Operation", NBTTagType.INT, 3);
//        attribute.set("UUIDLeast", NBTTagType.INT, (int) id.getLeastSignificantBits());
//        attribute.set("UUIDMost", NBTTagType.INT, (int) id.getMostSignificantBits());
//        attribute.set("Slot", NBTTagType.STRING, EquipmentSlot.OFF_HAND.name().toLowerCase().replace("_", ""));
//        // "mainhand" "offhand"
//
//        modifiers.add(attribute);
//        tag.set("AttributeModifiers", modifiers);
//        return setTag(item, tag);
//    }
}
