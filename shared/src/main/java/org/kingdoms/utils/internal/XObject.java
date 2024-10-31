package org.kingdoms.utils.internal;

import org.jetbrains.annotations.Nullable;

public final class XObject {
    private XObject() {}

    @SafeVarargs
    @Nullable
    public static <T> T firstNotNull(T... objects) {
        for (T object : objects) {
            if (object != null) return object;
        }

        return null;
    }
}
