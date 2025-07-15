package org.kingdoms.enginehub.worldedit;

import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class XClipboardFormatFactory {
    private static final boolean INTERFACE = ClipboardFormat.class.isInterface();

    @NotNull
    public static XClipboardFormat of(@NotNull ClipboardFormat clipboardFormat) {
        return INTERFACE ? new ClipboardFormatv7(clipboardFormat) : new ClipboardFormatv6(clipboardFormat);
    }

    @Nullable
    public static XClipboardFormat ofNullable(@Nullable ClipboardFormat clipboardFormat) {
        return clipboardFormat == null ? null : of(clipboardFormat);
    }
}
