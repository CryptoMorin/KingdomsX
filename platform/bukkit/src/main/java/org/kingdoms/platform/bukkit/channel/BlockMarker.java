package org.kingdoms.platform.bukkit.channel;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Duration;

public final class BlockMarker {
    public final Duration duration;
    public final Color color;
    public final String title;

    public BlockMarker(@NotNull Duration duration, @NotNull Color color, @NotNull String title) {
        this.duration = duration;
        this.color = color;
        this.title = title;
    }
}
