package org.kingdoms.editor;

public final class FileEntry {
    final String path;
    final byte[] bytes;

    public FileEntry(String path, byte[] bytes) {
        this.path = path;
        this.bytes = bytes;
    }
}
