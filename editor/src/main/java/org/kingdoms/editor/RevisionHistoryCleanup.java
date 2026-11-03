package org.kingdoms.editor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("NullableProblems")
public final class RevisionHistoryCleanup {
    public static List<Path> deleteOldFolders(Path rootFolder, Instant cutoff) throws IOException {
        List<Path> deletedFiles = new ArrayList<>();

        Files.walkFileTree(rootFolder, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(
                    Path dir, BasicFileAttributes attrs) throws IOException {

                // Never delete the root folder itself
                if (dir.equals(rootFolder)) {
                    return FileVisitResult.CONTINUE;
                }

                // Check folder's last modified time
                Instant lastModified =
                        Files.getLastModifiedTime(dir).toInstant();

                if (lastModified.isBefore(cutoff)) {
                    // Delete entire folder recursively
                    deletedFiles.add(dir);
                    deleteRecursively(dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return deletedFiles;
    }

    private static void deleteRecursively(Path folder) throws IOException {
        Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(
                    Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
