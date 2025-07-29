package org.kingdoms.utils.fs;

import com.google.common.base.Strings;
import org.kingdoms.utils.internal.arrays.ArrayUtils;
import org.kingdoms.utils.internal.functional.Fn;
import org.kingdoms.utils.internal.jdk.Java9;
import org.kingdoms.utils.internal.runnables.IORunnable;
import org.kingdoms.versioning.JavaVersion;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

public final class FSUtil {
    public static final StandardOpenOption[] STD_WRITER = {StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING};
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    private FSUtil() {}

    public static boolean isDirectAncestorOf(final Path parent, final Path child) {
        final Path absoluteParent = parent.toAbsolutePath().normalize();
        final Path absoluteChild = child.toAbsolutePath().normalize();
        return isSameFileAs(absoluteParent, absoluteChild.getParent());
    }

    public static boolean isAncestorOf(final Path parent, final Path child) {
        final Path absoluteParent = parent.toAbsolutePath().normalize();
        final Path absoluteChild = child.toAbsolutePath().normalize();

        if (absoluteParent.getNameCount() >= absoluteChild.getNameCount()) {
            return false;
        }

        final Path immediateParent = absoluteChild.getParent();
        if (immediateParent == null) {
            return false;
        }

        return isSameFileAs(absoluteParent, immediateParent) || isAncestorOf(absoluteParent, immediateParent);
    }

    private static boolean isSameFileAs(final Path path, final Path path2) {
        try {
            return Files.isSameFile(path, path2);
        } catch (final IOException ioe) {
            return path.toAbsolutePath().normalize().equals(path2.toAbsolutePath().normalize());
        }
    }

    /**
     * There's no better way in Java. We're bound by this stupid encapsulation of Java.
     */
    public static int countEntriesOf(Path folder) {
        if (!Files.isDirectory(folder))
            throw new IllegalArgumentException("Path is not a folder: " + folder.toAbsolutePath());
        try (DirectoryStream<Path> fs = Files.newDirectoryStream(folder)) {
            return ArrayUtils.sizeOfIterator(fs.iterator());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Mainly here because of Kotlin's stupid unnecessary checks and that array copy for varargs.
     */
    public static BufferedWriter standardWriter(Path path) throws IOException {
        return Files.newBufferedWriter(path, StandardCharsets.UTF_8, STD_WRITER);
    }

    public static int countEntriesOf(Path folder, Predicate<Path> filter) {
        if (!Files.isDirectory(folder))
            throw new IllegalArgumentException("Path is not a folder: " + folder.toAbsolutePath());
        try (DirectoryStream<Path> fs = Files.newDirectoryStream(folder)) {
            return (int) StreamSupport.stream(fs.spliterator(), false).filter(filter).count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path findSlotForCounterFile(Path folder, String prefix, String extension) {
        int counter = 1;

        Path file;
        do {
            file = folder.resolve(prefix + '-' + counter++ + '.' + extension);
        } while (Files.exists(file));

        return file;
    }

    public static Path findSlotForCounterFolder(Path folder, String prefix) {
        int counter = 1;

        Path file;
        do {
            file = folder.resolve(prefix + '-' + counter++);
        } while (Files.exists(file) && Files.isDirectory(file));

        return file;
    }

    /**
     * Similr to {@link #transfer(InputStream, OutputStream)} but first tries to lock "from"
     * before writing. This is only useful of "beforeWrite" parameter is used.
     * <p>
     * Mainly designed to
     */
    @SuppressWarnings("unused")
    public void lockBeforeCopy(Path from, OutputStream to, Runnable beforeWrite) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(from, StandardOpenOption.READ)) {
            try (FileLock lock = fileChannel.lock(0, Long.MAX_VALUE, true)) {
                WritableByteChannel zsChan = Channels.newChannel(to);
                // magic number for Windows, (64Mb - 32Kb)
                long maxCount = (64 * 1024 * 1024L) - (32 * 1024L);
                long size = fileChannel.size();
                long position = 0L;

                beforeWrite.run();
                while (size > 0) {
                    long count = fileChannel.transferTo(position, Math.min(maxCount, size), zsChan);
                    position += count;
                    size -= count;
                }
            }
        }
    }

    private static final Set<Character> ILLEGAL_CHARACTERS = new HashSet<>(Arrays.asList(
            // All
            '/', '\\', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '<', '>', '|', '\"', ':',

            // Unix Specific
            '\000',

            // Windows Specific
            '"', '*', '<', '>', '?', '|'
    ));

    private static boolean isInvalidFileNameChar(char ch) {
        return Character.isISOControl(ch) || ILLEGAL_CHARACTERS.contains(ch);
    }

    public static boolean isValidPath(String path) {
        if (Strings.isNullOrEmpty(path)) return false;
        try {
            Paths.get(path);
        } catch (InvalidPathException ex) {
            return false;
        }
        return true;
    }

    public static boolean isValidFileName(String name) {
        for (char ch : name.toCharArray()) {
            if (isInvalidFileNameChar(ch)) return false;
        }
        return true;
    }

    /**
     * If two {@link Path} are from different {@link FileSystem}s,
     * then you can't use methods like {@link Path#resolve(Path)} and {@link Path#relativize(Path)}
     * on them. This converts the given {@link Path} to the {@link FileSystem} which can be
     * taken from {@link Path#getFileSystem()}.
     */
    public static Path transformPath(FileSystem fs, Path path) {
        Path finalPath = fs.getPath(path.isAbsolute() ? fs.getSeparator() : "");
        for (Path component : path) {
            finalPath = finalPath.resolve(component.getFileName().toString());
        }
        return finalPath;
    }

    public static String removeInvalidFileChars(String name, String replaceWith) {
        StringBuilder builder = new StringBuilder();
        for (char ch : name.toCharArray()) {
            if (isInvalidFileNameChar(ch)) {
                builder.append(replaceWith);
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    public static String oneOfValidFileNames(String... names) {
        for (String name : names) {
            if (isValidFileName(name)) return name;
        }
        throw new IllegalArgumentException("None of the file names are valid: " + Arrays.toString(names));
    }

    public static boolean isFolderEmpty(Path folder) {
        return countEntriesOf(folder) == 0;
    }

    public static void deleteFolder(Path folder) {
        deleteFolder(folder, Fn.alwaysFalse());
    }

    /**
     * Deletes a folder entirely if it exists.
     */
    @SuppressWarnings("resource")
    public static void deleteFolder(Path folder, Predicate<Path> ignore) {
        if (!Files.exists(folder)) return;
        try {
            AtomicBoolean errored = new AtomicBoolean();
            Files.list(folder).forEach(path -> {
                try {
                    if (folder.equals(path)) return;
                    if (ignore.test(path)) return;

                    if (Files.isDirectory(path)) deleteFolder(path);
                    else Files.delete(path);
                } catch (IOException ex) {
                    errored.set(true);
                    ex.printStackTrace();
                }
            });
            if (!errored.get()) Files.delete(folder);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("resource")
    public static void deleteAllFileTypes(Path folder, String type) {
        try {
            Files.list(folder).forEach(path -> {
                try {
                    if (folder.equals(path)) return;
                    if (!path.toString().endsWith(type)) return; // path.endsWith() doesn't work it's fucking useless
                    if (Files.isDirectory(path)) return;
                    Files.delete(path);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Gets all the files in this folder and it's sub-folders.
     * None of the returned paths point to any of the folders, only the files.
     */
    public static List<Path> getFiles(Path folder) {
        List<Path> files = new ArrayList<>();
        PathIterator iterator = new PathIterator(
                null,
                (p, attrs) -> {if (attrs.isRegularFile()) files.add(p);}
        );
        try {
            Files.walkFileTree(folder, iterator);
            return files;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream stringToInputStream(String string) {
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }

    public static void transfer(InputStream in, OutputStream out) throws IOException {
        if (JavaVersion.supports(9)) {
            Java9.transferTo(in, out);
            return;
        }

        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(out, "out");

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, read);
        }
    }

    /**
     * Copied from {@link InputStream#readAllBytes()}
     */
    public static byte[] readAllBytes(InputStream input) throws IOException {
        if (JavaVersion.supports(9)) return Java9.readAllBytes(input);

        int len = MAX_BUFFER_SIZE;

        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }

        List<byte[]> bufs = null;
        byte[] result = null;
        int total = 0;
        int remaining = len;
        int n;
        do {
            byte[] buf = new byte[Math.min(remaining, DEFAULT_BUFFER_SIZE)];
            int nread = 0;

            // read to EOF which may read more or less than buffer size
            while ((n = input.read(buf, nread,
                    Math.min(buf.length - nread, remaining))) > 0) {
                nread += n;
                remaining -= n;
            }

            if (nread > 0) {
                if (MAX_BUFFER_SIZE - total < nread) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                if (nread < buf.length) {
                    buf = Arrays.copyOfRange(buf, 0, nread);
                }
                total += nread;
                if (result == null) {
                    result = buf;
                } else {
                    if (bufs == null) {
                        bufs = new ArrayList<>();
                        bufs.add(result);
                    }
                    bufs.add(buf);
                }
            }
            // if the last call to read returned -1 or the number of bytes
            // requested have been read then break
        } while (n >= 0 && remaining > 0);

        if (bufs == null) {
            if (result == null) {
                return new byte[0];
            }
            return result.length == total ?
                    result : Arrays.copyOf(result, total);
        }

        result = new byte[total];
        int offset = 0;
        remaining = total;
        for (byte[] b : bufs) {
            int count = Math.min(b.length, remaining);
            System.arraycopy(b, 0, result, offset, count);
            offset += count;
            remaining -= count;
        }

        return result;
    }

    public static void copyFolder(Path source, Path destination) {
        try {
            Files.walkFileTree(source, new CopyFileVisitor(source, destination));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Attempts to lock the entire file before transferring it to another stream.
     * This prevents doing things after making sure the file can be read at all,
     * mostly useful for putting zip enteries.
     *
     * @param beforeTransfer must be present. This method is useless without it.
     */
    @SuppressWarnings("unused")
    public static void lockAndTransfer(Path file, OutputStream transferTo, IORunnable beforeTransfer) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(file, StandardOpenOption.READ);
             FileLock lock = fileChannel.lock(0, Long.MAX_VALUE, true)) {
            WritableByteChannel zsChan = Channels.newChannel(transferTo);
            // magic number for Windows, (64Mb - 32Kb)
            long maxCount = (64 * 1024 * 1024L) - (32 * 1024L);
            long size = fileChannel.size();
            long position = 0L;

            beforeTransfer.run();
            while (size > 0) {
                long count = fileChannel.transferTo(position, Math.min(maxCount, size), zsChan);
                position += count;
                size -= count;
            }
        }
    }

    private static final class PathIterator extends SimpleFileVisitor<Path> {
        public final BiConsumer<Path, BasicFileAttributes> visitor;
        private final BiPredicate<Path, BasicFileAttributes> filter;

        private PathIterator(BiPredicate<Path, BasicFileAttributes> filter, BiConsumer<Path, BasicFileAttributes> consumer) {
            this.visitor = consumer;
            this.filter = filter;
        }

        private FileVisitResult visit(Path path, BasicFileAttributes attrs) {
            if (filter == null || filter.test(path, attrs)) {
                visitor.accept(path, attrs);
                return FileVisitResult.CONTINUE;
            }
            return FileVisitResult.SKIP_SUBTREE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return visit(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            return visit(file, attrs);
        }
    }

    private static final class CopyFileVisitor extends SimpleFileVisitor<Path> {
        private final Path sourcePath, targetPath;

        public CopyFileVisitor(Path sourcePath, Path targetPath) {
            this.sourcePath = sourcePath;
            this.targetPath = targetPath;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir,
                                                 final BasicFileAttributes attrs) throws IOException {
            Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file,
                                         final BasicFileAttributes attrs) throws IOException {
            Files.copy(file, targetPath.resolve(sourcePath.relativize(file)));
            return FileVisitResult.CONTINUE;
        }
    }
}
