package org.kingdoms.editor;

import java.nio.file.Path;
import java.util.List;

public interface KingdomsEditorListener {
    default void onReady() {}

    void close(int code, String reason);

    /**
     * If the files were applied successfully or false if there are conflicts.
     */
    boolean applyRevision(Path revisionOutputDirectory, List<FileEntry> entries);

    void debug(String message);
}
