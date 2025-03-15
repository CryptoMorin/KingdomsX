package org.kingdoms.platform.bukkit.adapters;

import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftMapping;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kingdoms.nbt.NBTConverter;
import org.kingdoms.nbt.NBTTagConverterRegistry;
import org.kingdoms.nbt.NBTTagId;
import org.kingdoms.nbt.tag.NBTTag;
import org.kingdoms.nbt.tag.NBTTagCompound;
import org.kingdoms.nbt.tag.NBTTagType;
import org.kingdoms.utils.internal.functional.Fn;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.kingdoms.utils.internal.reflection.Reflect.getDeclaredField;

public final class BukkitNBTAdapter {
    public static void registerAll() {
        List<NBTConverter<? extends NBTTag<?>, Object>> list = Arrays.asList(
                new BukkitNBTCompound(), new NBTTagList<>(), new NBTTagString(),
                new NBTTagByte(), new NBTTagShort(), new NBTTagInt(), new NBTTagFloat(), new NBTTagLong(), new NBTTagDouble(),
                new NBTTagIntArray(), new NBTTagByteArray(), new NBTTagLongArray(),
                new NBTTagEnd()
        );
        for (NBTConverter<? extends NBTTag<?>, Object> converter : list) {
            NBTTagConverterRegistry.INSTANCE.register(converter);
        }
    }

    private static Class<?> getNBTBaseClass() {
        MinecraftClassHandle base = XReflection.ofMinecraft()
                .inPackage(MinecraftPackage.NMS, "nbt")
                .map(MinecraftMapping.SPIGOT, "NBTBase");
        if (XReflection.SUPPORTED_MAPPINGS.contains(MinecraftMapping.MOJANG)) {
            base.map(MinecraftMapping.MOJANG, "Tag");
        }

        return base.unreflect();
    }

    @Nullable
    private static Class<?> getNBTClass(NBTTagId type) {
        // Capitalize
        String typeName = Arrays.stream(type.name().split("_"))
                .map(t -> t.charAt(0) + t.substring(1).toLowerCase())
                .collect(Collectors.joining(""));
        return XReflection.ofMinecraft()
                .inPackage(MinecraftPackage.NMS, "nbt")
                .map(MinecraftMapping.MOJANG, typeName + "Tag")
                .map(MinecraftMapping.SPIGOT, "NBTTag" + typeName)
                .reflectOrNull();
    }

    private static final class BukkitNBTCompound implements NBTConverter<NBTTagCompound, Object> {
        private static final MethodHandle NBT_TAG_COMPOUND_CONSTRUCTOR;
        private static final MethodHandle GET_COMPOUND_MAP, SET_COMPOUND_MAP;

        static {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> nbtCompound = getNBTClass(NBTTagId.COMPOUND);
            MethodHandle handler = null, getMap = null, setMap = null;

            try {
                Field field = getDeclaredField(nbtCompound, "x", "map");
                field.setAccessible(true);
                getMap = lookup.unreflectGetter(field);

                if (XReflection.supports(15)) {
                    Constructor<?> ctor = nbtCompound.getDeclaredConstructor(Map.class);
                    ctor.setAccessible(true);
                    handler = lookup.unreflectConstructor(ctor);
                } else {
                    handler = lookup.findConstructor(nbtCompound, MethodType.methodType(void.class));
                    setMap = lookup.unreflectSetter(field);
                }
            } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }

            NBT_TAG_COMPOUND_CONSTRUCTOR = handler;
            GET_COMPOUND_MAP = getMap;
            SET_COMPOUND_MAP = setMap;
        }

        @SuppressWarnings("unchecked")
        public static Map<String, Object> getRawMap(Object nbtObject) {
            try {
                return (Map<String, Object>) GET_COMPOUND_MAP.invoke(nbtObject);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @NotNull
        @Override
        public NBTTagType<NBTTagCompound> getType() {
            return NBTTagType.COMPOUND;
        }

        @NotNull
        @Override
        public NBTTagCompound fromNBT(Object tag) {
            try {
                NBTTagCompound builder = NBTTagCompound.empty();
                Map<String, Object> baseMap = getRawMap(tag);
                for (Map.Entry<String, Object> base : baseMap.entrySet()) {
                    NBTConverter<NBTTag<?>, ?> converter = NBTTagConverterRegistry.INSTANCE.get(NBTTagId.fromClassNameOfObject(base.getValue()));
                    builder.set(base.getKey(), converter.fromNBT(Fn.cast(base.getValue())));
                }
                return builder;
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public Object toNBT(@NotNull NBTTagCompound tag) {
            try {
                Map<String, Object> map = new HashMap<>(tag.value().size());
                for (Map.Entry<String, ? extends NBTTag<?>> entry : tag.value().entrySet()) {
                    if (entry.getValue() == tag) throw new IllegalStateException("Recursive NBT");
                    NBTConverter<NBTTag<?>, ?> converter = NBTTagConverterRegistry.INSTANCE.get(entry.getValue().type().id());
                    map.put(entry.getKey(), converter.toNBT(entry.getValue()));
                }

                Object compound;
                if (XReflection.supports(15)) compound = NBT_TAG_COMPOUND_CONSTRUCTOR.invoke(map);
                else {
                    compound = NBT_TAG_COMPOUND_CONSTRUCTOR.invoke();
                    SET_COMPOUND_MAP.invoke(compound, map);
                }
                return compound;
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private static final class NBTTagEnd implements NBTConverter<org.kingdoms.nbt.tag.NBTTagEnd, Object> {
        private static final Object INSTANCE;

        static {
            Class<?> clazz = getNBTClass(NBTTagId.END);
            Object instance = null;

            if (clazz != null) {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                try {
                    // public static final NBTTagEnd b = new NBTTagEnd();
                    instance = lookup.findStaticGetter(clazz, "b", clazz).invoke();
                } catch (Throwable e) {
                    if (XReflection.supports(13)) e.printStackTrace();
                }
            }

            INSTANCE = instance;
        }

        @NotNull
        @Override
        public NBTTagType<org.kingdoms.nbt.tag.NBTTagEnd> getType() {
            return NBTTagType.END;
        }

        @NotNull
        @Override
        public org.kingdoms.nbt.tag.NBTTagEnd fromNBT(Object tag) {
            return org.kingdoms.nbt.tag.NBTTagEnd.instance();
        }

        @Override
        public Object toNBT(@NotNull org.kingdoms.nbt.tag.NBTTagEnd tag) {
            return INSTANCE;
        }
    }

    private static final class NBTTagString implements NBTConverter<org.kingdoms.nbt.tag.NBTTagString, Object> {
        private static final MethodHandle CONSTRUCTOR;
        private static final MethodHandle NBT_DATA;

        static {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> clazz = getNBTClass(NBTTagId.STRING);
            MethodHandle handler = null, data = null;

            try {
                if (XReflection.supports(15))
                    handler = lookup.findStatic(clazz, "a", MethodType.methodType(clazz, String.class));
                else handler = lookup.findConstructor(clazz, MethodType.methodType(void.class, String.class));

                Field field = getDeclaredField(clazz, "A", "data");
                field.setAccessible(true);
                data = lookup.unreflectGetter(field);
            } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }

            CONSTRUCTOR = handler;
            NBT_DATA = data;
        }

        @NotNull
        @Override
        public NBTTagType<org.kingdoms.nbt.tag.NBTTagString> getType() {
            return NBTTagType.STRING;
        }

        @Override
        public Object toNBT(@NotNull org.kingdoms.nbt.tag.NBTTagString tag) {
            try {
                return CONSTRUCTOR.invoke(tag.value());
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @NotNull
        @Override
        public org.kingdoms.nbt.tag.NBTTagString fromNBT(Object tag) {
            try {
                return org.kingdoms.nbt.tag.NBTTagString.of((String) NBT_DATA.invoke(tag));
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private static final class NBTTagLongArray implements NBTConverter<org.kingdoms.nbt.tag.NBTTagLongArray, Object> {
        private static final MethodHandle CONSTRUCTOR;
        private static final MethodHandle NBT_DATA;

        static {
            Class<?> clazz = getNBTClass(NBTTagId.LONG_ARRAY);
            MethodHandle handler = null, data = null;

            if (clazz != null) {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                try {
                    handler = lookup.findConstructor(clazz, MethodType.methodType(void.class, long[].class));

                    data = XReflection.of(clazz).field("private long[] data;").getter()
                            .named("c", /* 1.16.5 */ "b")
                            .unreflect();
                } catch (NoSuchMethodException | IllegalAccessException ex) {
                    if (XReflection.supports(13)) ex.printStackTrace();
                }
            }

            CONSTRUCTOR = handler;
            NBT_DATA = data;
        }

        @NotNull
        @Override
        public NBTTagType<org.kingdoms.nbt.tag.NBTTagLongArray> getType() {
            return NBTTagType.LONG_ARRAY;
        }

        @NotNull
        @Override
        public org.kingdoms.nbt.tag.NBTTagLongArray fromNBT(Object tag) {
            try {
                return org.kingdoms.nbt.tag.NBTTagLongArray.of((long[]) NBT_DATA.invoke(tag));
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public Object toNBT(@NotNull org.kingdoms.nbt.tag.NBTTagLongArray tag) {
            try {
                return CONSTRUCTOR.invoke(tag.value());
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private static final class NBTTagIntArray implements NBTConverter<org.kingdoms.nbt.tag.NBTTagIntArray, Object> {
        private static final MethodHandle CONSTRUCTOR;
        private static final MethodHandle NBT_DATA;

        static {
            Class<?> clazz = getNBTClass(NBTTagId.INT_ARRAY);
            MethodHandle handler = null, data = null;

            if (clazz != null) {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                try {
                    handler = lookup.findConstructor(clazz, MethodType.methodType(void.class, int[].class));

                    Field field = getDeclaredField(clazz, "c", "data");
                    field.setAccessible(true);
                    data = lookup.unreflectGetter(field);
                } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }

            CONSTRUCTOR = handler;
            NBT_DATA = data;
        }

        @NotNull
        @Override
        public NBTTagType<org.kingdoms.nbt.tag.NBTTagIntArray> getType() {
            return NBTTagType.INT_ARRAY;
        }

        @NotNull
        @Override
        public org.kingdoms.nbt.tag.NBTTagIntArray fromNBT(Object tag) {
            try {
                return org.kingdoms.nbt.tag.NBTTagIntArray.of((int[]) NBT_DATA.invoke(tag));
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public Object toNBT(@NotNull org.kingdoms.nbt.tag.NBTTagIntArray tag) {
            try {
                return CONSTRUCTOR.invoke(tag.value());
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private static final class NBTTagList<T extends NBTTag<?>> implements NBTConverter<org.kingdoms.nbt.tag.NBTTagList<T>, Object> {
        private static final MethodHandle CONSTRUCTOR;
        private static final MethodHandle GET_DATA, SET_DATA;
        private static final MethodHandle GET_TYPE_ID, SET_TYPE_ID;

        static {
            Class<?> clazz = getNBTClass(NBTTagId.LIST);
            Class<?> nbtBase = getNBTBaseClass();
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handler = null, getData = null, setData = null, getTypeId = null, setTypeId = null;

            try {
                Field field = getDeclaredField(clazz, "c", "list");
                field.setAccessible(true);
                getData = lookup.unreflectGetter(field);

                if (XReflection.supports(15)) {
                    Constructor<?> ctor = clazz.getDeclaredConstructor(List.class, byte.class);
                    ctor.setAccessible(true);
                    handler = lookup.unreflectConstructor(ctor);
                } else {
                    handler = lookup.findConstructor(clazz, MethodType.methodType(void.class));
                    setData = lookup.unreflectSetter(field);
                }

                getTypeId = lookup.findVirtual(nbtBase,
                        XReflection.v(19, "b").v(18, "a").orElse("getTypeId"),
                        MethodType.methodType(byte.class));

                // Obfuscated:
                //   * v1.8 - v1.12 : d
                //   * v1.13        : h
                //   * v1.14        : g
                //   * v1.15        : h
                // From v1.15+ there's a constructor we can use instead.
                setTypeId = XReflection.of(clazz).field("private byte type").setter().reflect();
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }

            CONSTRUCTOR = handler;
            GET_DATA = getData;
            SET_DATA = setData;
            GET_TYPE_ID = getTypeId;
            SET_TYPE_ID = setTypeId;
        }

        @NotNull
        @Override
        public NBTTagType<org.kingdoms.nbt.tag.NBTTagList<T>> getType() {
            return NBTTagType.listOf();
        }

        @NotNull
        @Override
        public org.kingdoms.nbt.tag.NBTTagList<T> fromNBT(Object tag) {
            List<?> nbtList;
            try {
                nbtList = (List<?>) GET_DATA.invoke(tag);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }

            List<T> converted = new ArrayList<>();
            NBTTagType<T> type = null;
            for (Object element : nbtList) {
                if (type == null) type = NBTTagType.fromId(NBTTagId.fromClassNameOfObject(element));
                converted.add(BukkitAdapter.adapt(type, element));
            }

            return org.kingdoms.nbt.tag.NBTTagList.of(type, converted);
        }

        @Override
        public Object toNBT(@NotNull org.kingdoms.nbt.tag.NBTTagList<T> tag) {
            try {
                List<Object> array = new ArrayList<>(tag.value().size());
                for (T base : tag.value()) array.add(BukkitAdapter.adapt(base));

                // If the list id is not set correctly, there are list methods that may
                // ignore the list completely and return an empty list.
                if (XReflection.supports(15)) {
                    byte typeId = array.isEmpty() ? 0 : (byte) GET_TYPE_ID.invoke(array.get(0));
                    return CONSTRUCTOR.invoke(array, typeId);
                } else {
                    Object nbtList = CONSTRUCTOR.invoke();
                    SET_DATA.invoke(nbtList, array);
                    SET_TYPE_ID.invoke(nbtList, (byte) tag.elementType().id().id());
                    return nbtList;
                }
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private static final class NBTTagDouble implements NBTConverter<org.kingdoms.nbt.tag.NBTTagDouble, Object> {
        private static final MethodHandle CONSTRUCTOR;
        private static final MethodHandle NBT_DATA;

        static {
            Class<?> clazz = getNBTClass(NBTTagId.DOUBLE);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handler = null, data = null;

            try {
                if (XReflection.supports(15))
                    handler = lookup.findStatic(clazz, "a", MethodType.methodType(clazz, double.class));
                else handler = lookup.findConstructor(clazz, MethodType.methodType(void.class, double.class));

                Field field = getDeclaredField(clazz, "w", "data");
                field.setAccessible(true);
                data = lookup.unreflectGetter(field);
            } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }

            CONSTRUCTOR = handler;
            NBT_DATA = data;
        }

        @NotNull
        @Override
        public NBTTagType<org.kingdoms.nbt.tag.NBTTagDouble> getType() {
            return NBTTagType.DOUBLE;
        }

        @NotNull
        @Override
        public org.kingdoms.nbt.tag.NBTTagDouble fromNBT(Object tag) {
            try {
                return org.kingdoms.nbt.tag.NBTTagDouble.of((double) NBT_DATA.invoke(tag));
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public Object toNBT(@NotNull org.kingdoms.nbt.tag.NBTTagDouble tag) {
            try {
                return CONSTRUCTOR.invoke(tag.valueAsDouble());
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private static final class NBTTagInt implements NBTConverter<org.kingdoms.nbt.tag.NBTTagInt, Object> {
        private static final MethodHandle CONSTRUCTOR;
        private static final MethodHandle NBT_DATA;

        static {
            Class<?> clazz = getNBTClass(NBTTagId.INT);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handler = null, data = null;

            try {
                if (XReflection.supports(15)) {
                    handler = lookup.findStatic(clazz, "a", MethodType.methodType(clazz, int.class));
                } else {
                    handler = lookup.findConstructor(clazz, MethodType.methodType(void.class, int.class));
                }

                Field field = getDeclaredField(clazz, "c", "data");
                field.setAccessible(true);
                data = lookup.unreflectGetter(field);
            } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }

            CONSTRUCTOR = handler;
            NBT_DATA = data;
        }

        @NotNull
        @Override
        public NBTTagType<org.kingdoms.nbt.tag.NBTTagInt> getType() {
            return NBTTagType.INT;
        }

        @NotNull
        @Override
        public org.kingdoms.nbt.tag.NBTTagInt fromNBT(Object tag) {
            try {
                return org.kingdoms.nbt.tag.NBTTagInt.of((int) NBT_DATA.invoke(tag));
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public Object toNBT(@NotNull org.kingdoms.nbt.tag.NBTTagInt tag) {
            try {
                return CONSTRUCTOR.invoke(tag.valueAsInt());
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private static final class NBTTagByte implements NBTConverter<org.kingdoms.nbt.tag.NBTTagByte, Object> {
        private static final MethodHandle CONSTRUCTOR;
        private static final MethodHandle NBT_DATA;

        static {
            Class<?> clazz = getNBTClass(NBTTagId.BYTE);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handler = null, data = null;

            try {
                if (XReflection.supports(15)) {
                    handler = lookup.findStatic(clazz, "a", MethodType.methodType(clazz, byte.class));
                } else {
                    handler = lookup.findConstructor(clazz, MethodType.methodType(void.class, byte.class));
                }

                Field field = getDeclaredField(clazz, "x", "data");
                field.setAccessible(true);
                data = lookup.unreflectGetter(field);
            } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }

            CONSTRUCTOR = handler;
            NBT_DATA = data;
        }


        @NotNull
        @Override
        public NBTTagType<org.kingdoms.nbt.tag.NBTTagByte> getType() {
            return NBTTagType.BYTE;
        }

        @NotNull
        @Override
        public org.kingdoms.nbt.tag.NBTTagByte fromNBT(Object tag) {
            try {
                return org.kingdoms.nbt.tag.NBTTagByte.of((byte) NBT_DATA.invoke(tag));
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public Object toNBT(@NotNull org.kingdoms.nbt.tag.NBTTagByte tag) {
            try {
                return CONSTRUCTOR.invoke(tag.valueAsByte());
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private static final class NBTTagByteArray implements NBTConverter<org.kingdoms.nbt.tag.NBTTagByteArray, Object> {
        private static final MethodHandle CONSTRUCTOR;
        private static final MethodHandle NBT_DATA;

        static {
            Class<?> clazz = getNBTClass(NBTTagId.BYTE_ARRAY);
            MethodHandle handler = null, data = null;

            if (clazz != null) {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                try {
                    handler = lookup.findConstructor(clazz, MethodType.methodType(void.class, byte[].class));

                    Field field = getDeclaredField(clazz, "c", "data");
                    field.setAccessible(true);
                    data = lookup.unreflectGetter(field);
                } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }

            CONSTRUCTOR = handler;
            NBT_DATA = data;
        }

        @NotNull
        @Override
        public NBTTagType<org.kingdoms.nbt.tag.NBTTagByteArray> getType() {
            return NBTTagType.BYTE_ARRAY;
        }

        @NotNull
        @Override
        public org.kingdoms.nbt.tag.NBTTagByteArray fromNBT(Object tag) {
            try {
                return tag == null ?
                        org.kingdoms.nbt.tag.NBTTagByteArray.of() :
                        org.kingdoms.nbt.tag.NBTTagByteArray.of(((byte[]) NBT_DATA.invoke(tag)));
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public Object toNBT(@NotNull org.kingdoms.nbt.tag.NBTTagByteArray tag) {
            try {
                return CONSTRUCTOR.invoke(tag.value());
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private static final class NBTTagShort implements NBTConverter<org.kingdoms.nbt.tag.NBTTagShort, Object> {
        private static final MethodHandle CONSTRUCTOR;
        private static final MethodHandle NBT_DATA;

        static {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> clazz = getNBTClass(NBTTagId.SHORT);
            MethodHandle handler = null, data = null;

            try {
                if (XReflection.supports(15)) {
                    handler = lookup.findStatic(clazz, "a", MethodType.methodType(clazz, short.class));
                } else {
                    handler = lookup.findConstructor(clazz, MethodType.methodType(void.class, short.class));
                }

                Field field = getDeclaredField(clazz, "c", "data");
                field.setAccessible(true);
                data = lookup.unreflectGetter(field);
            } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }

            CONSTRUCTOR = handler;
            NBT_DATA = data;
        }

        @NotNull
        @Override
        public NBTTagType<org.kingdoms.nbt.tag.NBTTagShort> getType() {
            return NBTTagType.SHORT;
        }

        @NotNull
        @Override
        public org.kingdoms.nbt.tag.NBTTagShort fromNBT(Object tag) {
            try {
                return org.kingdoms.nbt.tag.NBTTagShort.of((short) NBT_DATA.invoke(tag));
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public Object toNBT(@NotNull org.kingdoms.nbt.tag.NBTTagShort tag) {
            try {
                return CONSTRUCTOR.invoke(tag.valueAsShort());
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private static final class NBTTagLong implements NBTConverter<org.kingdoms.nbt.tag.NBTTagLong, Object> {
        private static final MethodHandle CONSTRUCTOR;
        private static final MethodHandle NBT_DATA;

        static {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> clazz = getNBTClass(NBTTagId.LONG);
            MethodHandle handler = null, data = null;

            try {
                if (XReflection.supports(15))
                    handler = lookup.findStatic(clazz, "a", MethodType.methodType(clazz, long.class));
                else handler = lookup.findConstructor(clazz, MethodType.methodType(void.class, long.class));

                Field field = getDeclaredField(clazz, "c", "data");
                field.setAccessible(true);
                data = lookup.unreflectGetter(field);
            } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }

            CONSTRUCTOR = handler;
            NBT_DATA = data;
        }


        @NotNull
        @Override
        public NBTTagType<org.kingdoms.nbt.tag.NBTTagLong> getType() {
            return NBTTagType.LONG;
        }

        @NotNull
        @Override
        public org.kingdoms.nbt.tag.NBTTagLong fromNBT(Object tag) {
            try {
                return org.kingdoms.nbt.tag.NBTTagLong.of((long) NBT_DATA.invoke(tag));
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public Object toNBT(@NotNull org.kingdoms.nbt.tag.NBTTagLong tag) {
            try {
                return CONSTRUCTOR.invoke(tag.valueAsLong());
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private static final class NBTTagFloat implements NBTConverter<org.kingdoms.nbt.tag.NBTTagFloat, Object> {
        private static final MethodHandle CONSTRUCTOR;
        private static final MethodHandle NBT_DATA;

        static {
            Class<?> clazz = getNBTClass(NBTTagId.FLOAT);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handler = null, data = null;

            try {
                if (XReflection.supports(15))
                    handler = lookup.findStatic(clazz, "a", MethodType.methodType(clazz, float.class));
                else handler = lookup.findConstructor(clazz, MethodType.methodType(void.class, float.class));

                Field field = getDeclaredField(clazz, "w", "data");
                field.setAccessible(true);
                data = lookup.unreflectGetter(field);
            } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }

            CONSTRUCTOR = handler;
            NBT_DATA = data;
        }

        @NotNull
        @Override
        public NBTTagType<org.kingdoms.nbt.tag.NBTTagFloat> getType() {
            return NBTTagType.FLOAT;
        }

        @NotNull
        @Override
        public org.kingdoms.nbt.tag.NBTTagFloat fromNBT(Object tag) {
            try {
                return org.kingdoms.nbt.tag.NBTTagFloat.of((float) NBT_DATA.invoke(tag));
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public Object toNBT(@NotNull org.kingdoms.nbt.tag.NBTTagFloat tag) {
            try {
                return CONSTRUCTOR.invoke(tag.valueAsFloat());
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }
}
