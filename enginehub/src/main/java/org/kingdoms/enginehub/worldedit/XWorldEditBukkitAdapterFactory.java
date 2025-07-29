package org.kingdoms.enginehub.worldedit;

import org.kingdoms.utils.internal.reflection.Reflect;

public final class XWorldEditBukkitAdapterFactory {
    public static final XWorldEditBukkitAdapter INSTANCE;
    private static final boolean v6 = !Reflect.classExists("com.sk89q.worldedit.world.block.BlockStateHolder");

    static {
        INSTANCE = v6 ? new WorldEditBukkitAdapterv6() : new WorldEditBukkitAdapterv7();
    }

    public static boolean v6() {
        return v6;
    }
}
