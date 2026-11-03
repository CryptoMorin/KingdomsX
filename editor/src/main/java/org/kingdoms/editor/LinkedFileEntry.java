package org.kingdoms.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

public final class LinkedFileEntry {
    public enum FileRevisionResult {
        ADDED, REMOVED, CHANGED, CONFLICT;
    }

    final byte[] newBytes;
    final Path diskPath;
    final String relativePath;
    private final FileRevisionResult result;

    public LinkedFileEntry(byte[] newBytes, Path diskPath, String relativePath, FileRevisionResult result) {
        this.newBytes = newBytes;
        this.diskPath = diskPath;
        this.relativePath = relativePath;
        this.result = result;
    }

    public FileRevisionResult getFileRevision() {
        return this.result;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public Path getDiskPath() {
        return diskPath;
    }

    public void applyChanges(KingdomsEditorSession.WorkspaceSource src) throws IOException {
        switch (result) {
            case REMOVED:
                src.files.remove(relativePath);
                Files.delete(diskPath);
                break;

            case ADDED:
            case CONFLICT:
                Path parent = diskPath.getParent();

                if (parent != null) {
                    Files.createDirectories(parent);
                }
                // fall-through and write the file

            case CHANGED:
                src.files.put(relativePath, KingdomsEditorSecurity.sha256Hex(newBytes));
                Files.write(diskPath, newBytes);
        }
    }

    public static List<LinkedFileEntry> compareFiles(
            List<FileEntry> entries,
            Path rootPath,
            KingdomsEditorSession.WorkspaceSource source,
            Predicate<Path> fileTest
    ) throws IOException {

        List<LinkedFileEntry> linkedEntries = new ArrayList<>();
        Path root = rootPath.toAbsolutePath().normalize();

        Map<String, byte[]> pendingFiles = new HashMap<>();
        for (FileEntry entry : entries) {
            String normalizedRelativePath = entry.path
                    .replace('\\', '/')
                    .replaceFirst("^/+", "");
            pendingFiles.put(normalizedRelativePath, entry.bytes);
        }

        for (Map.Entry<String, String> fileEntry : source.files.entrySet()) {
            String relativePathStr = fileEntry.getKey();

            if (relativePathStr.equals(KingdomsEditorSession.MANIFEST_PATH)) continue;

            Path diskPath = root.resolve(relativePathStr).normalize();

            // Prevent paths such as "../../outside.txt" from escaping rootPath.
            if (!diskPath.startsWith(root)) {
                throw new IllegalArgumentException(
                        "File path escapes root directory: " + diskPath
                );
            }

            byte[] serverBytes = pendingFiles.remove(relativePathStr);

            String currentSHA;
            boolean different;
            if (Files.exists(diskPath)) {
                byte[] currentBytes = Files.readAllBytes(diskPath);
                currentSHA = KingdomsEditorSecurity.sha256Hex(currentBytes);
                different = serverBytes == null || !Arrays.equals(serverBytes, currentBytes);
            } else {
                currentSHA = null;
                different = serverBytes != null; // REMOVED + REMOVED -> Not changed
            }

            if (different) {
                FileRevisionResult result;
                if (serverBytes == null) result = FileRevisionResult.REMOVED;
                else if (!Files.exists(diskPath)) result = FileRevisionResult.ADDED;
                else if (!Objects.equals(currentSHA, fileEntry.getValue())) result = FileRevisionResult.CONFLICT;
                else result = FileRevisionResult.CHANGED;

                linkedEntries.add(new LinkedFileEntry(
                        serverBytes,
                        diskPath,
                        relativePathStr,
                        result
                ));
            }
        }

        // Check for files that were received from the server, but were not included in the
        // original source.
        for (Map.Entry<String, byte[]> newFiles : pendingFiles.entrySet()) {
            String relativePathStr = newFiles.getKey();
            if (relativePathStr.equals(KingdomsEditorSession.MANIFEST_PATH)) continue;

            Path diskPath = root.resolve(relativePathStr).normalize();
            FileRevisionResult result;

            if (Files.exists(diskPath)) {
                // The file exists but was not included in the original source.
                if (Arrays.equals(newFiles.getValue(), Files.readAllBytes(diskPath))) {
                    // We will let it slide, since the files are exactly the same.
                    result = null;
                } else {
                    result = FileRevisionResult.CONFLICT;
                }
            } else {
                result = FileRevisionResult.ADDED;
            }

            if (result == null) continue;

            linkedEntries.add(new LinkedFileEntry(
                    newFiles.getValue(),
                    diskPath,
                    relativePathStr,
                    result
            ));
        }

        return linkedEntries;
    }
}