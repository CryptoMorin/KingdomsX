package org.kingdoms.editor;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.*;

final class WorkspaceSession {
    private static final long MAX_YAML_BYTES = 10L * 1024 * 1024;
    private static final long MAX_SCHEMATIC_BYTES = 25L * 1024 * 1024;
    private static final long MAX_EXPANDED_BYTES = 100L * 1024 * 1024;
    private static final int MAX_ARCHIVE_FILES = 1000;

    private static final Pattern YAML_FILE = Pattern.compile("\\.ya?ml$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCHEMATIC_FILE = Pattern.compile("\\.(?:schematic|schem)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DRIVE_PREFIX = Pattern.compile("^[A-Za-z]:");

    private WorkspaceSession() {}

    public static List<FileEntry> extractWorkspaceArchive(byte[] bytes)
            throws Exception {

        List<FileEntry> entries =
                new ArrayList<>();
        Map<String, String> paths = new HashMap<>();
        int fileCount = 0;
        long expandedBytes = 0;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                String path;
                try {
                    path = safeArchivePath(ze.getName());
                    if (path.endsWith("/")) {
                        zis.closeEntry();
                        continue;
                    }

                    fileCount++;
                    if (fileCount > MAX_ARCHIVE_FILES) {
                        throw new IllegalArgumentException(
                                "This ZIP archive contains more than "
                                        + String.format("%,d", MAX_ARCHIVE_FILES) + " files.");
                    }

                    long declared = ze.getSize();
                    if (declared > 0 && YAML_FILE.matcher(path).find() && declared > MAX_YAML_BYTES) {
                        throw new IllegalArgumentException(
                                path + " is larger than the 10 MiB per-file limit.");
                    }
                    if (declared > 0 && SCHEMATIC_FILE.matcher(path).find()
                            && declared > MAX_SCHEMATIC_BYTES) {
                        throw new IllegalArgumentException(
                                path + " is larger than the 25 MiB per-file limit.");
                    }

                    String identity = path.toLowerCase(Locale.US);
                    if (paths.containsKey(identity)) {
                        throw new IllegalArgumentException(
                                "This ZIP archive contains the path " + path + " more than once.");
                    }
                    paths.put(identity, path);
                } catch (IllegalArgumentException e) {
                    throw e;
                }

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                long fileBytes = 0;
                while ((n = zis.read(buf)) != -1) {
                    fileBytes += n;
                    expandedBytes += n;
                    if (expandedBytes > MAX_EXPANDED_BYTES) {
                        throw new IllegalArgumentException(
                                "This ZIP expands beyond the 100 MiB limit.");
                    }
                    if (YAML_FILE.matcher(path).find() && fileBytes > MAX_YAML_BYTES) {
                        throw new IllegalArgumentException(
                                path + " is larger than the 10 MiB per-file limit.");
                    }
                    if (SCHEMATIC_FILE.matcher(path).find() && fileBytes > MAX_SCHEMATIC_BYTES) {
                        throw new IllegalArgumentException(
                                path + " is larger than the 25 MiB per-file limit.");
                    }
                    bos.write(buf, 0, n);
                }
                entries.add(new FileEntry(path, bos.toByteArray()));
                zis.closeEntry();
            }
        }
        return entries;
    }

    private static String safeArchivePath(String input) {
        String path = String.valueOf(input).replace('\\', '/');
        if (path.isEmpty() || path.indexOf('\0') >= 0 || path.startsWith("/")
                || DRIVE_PREFIX.matcher(path).find()) {
            throw new IllegalArgumentException(
                    "The selection contains an unsafe or invalid file path.");
        }
        boolean directory = path.endsWith("/");
        String body = directory ? path.substring(0, path.length() - 1) : path;
        String[] segments = body.split("/");
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("The path " + path + " is not safe to open.");
            }
        }
        return directory ? body + "/" : body;
    }
}