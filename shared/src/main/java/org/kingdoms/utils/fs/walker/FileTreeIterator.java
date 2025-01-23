package org.kingdoms.utils.fs.walker;

import org.kingdoms.utils.fs.walker.visitors.PathVisit;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An {@code Iterator} to iterate over the nodes of a file tree.
 * <p>
 * {@snippet lang = java:
 *     try (FileTreeIterator iterator = new FileTreeIterator(start, maxDepth, options)) {
 *         while (iterator.hasNext()) {
 *             Event ev = iterator.next();
 *             Path path = ev.file();
 *             BasicFileAttributes attrs = ev.attributes();
 *         }
 *     }
 *}
 */

class FileTreeIterator implements Iterator<PathVisit>, Closeable, FileWalkerController {
    private final FileTreeWalker walker;
    private PathVisit next;

    /**
     * Creates a new iterator to walk the file tree starting at the given file.
     *
     * @throws IllegalArgumentException if {@code maxDepth} is negative
     * @throws IOException              if an I/O errors occurs opening the starting file
     * @throws SecurityException        if the security manager denies access to the starting file
     * @throws NullPointerException     if {@code start} or {@code options} is {@code null} or
     *                                  the options array contains a {@code null} element
     */
    FileTreeIterator(Path start, int maxDepth, FileVisitOption... options) throws IOException {
        this.walker = new FileTreeWalker(Arrays.asList(options), maxDepth);
        this.next = walker.walk(start);
        assert next.getVisitType() == PathVisit.Type.ENTRY ||
                next.getVisitType() == PathVisit.Type.START_DIRECTORY;

        IOException ioe = next.getException();
        if (ioe != null) throw ioe;
    }

    private void fetchNextIfNeeded() {
        if (next == null) {
            PathVisit ev = walker.next();
            while (ev != null) {
                IOException ioe = ev.getException();
                if (ioe != null)
                    throw new UncheckedIOException(ioe);

                // END_DIRECTORY events are ignored
                if (ev.getVisitType() != PathVisit.Type.END_DIRECTORY) {
                    next = ev;
                    return;
                }
                ev = walker.next();
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (!walker.isOpen())
            throw new IllegalStateException();

        fetchNextIfNeeded();
        return next != null;
    }

    @Override
    public PathVisit next() {
        if (!walker.isOpen())
            throw new IllegalStateException();

        fetchNextIfNeeded();
        if (next == null)
            throw new NoSuchElementException();

        PathVisit result = next;
        next = null;
        return result;
    }

    @Override
    public void skipDirectory() {
        walker.skipDirectory();
    }

    @Override
    public void skipRemainingSiblings() {
        walker.skipRemainingSiblings();
    }

    @Override
    public void close() {
        walker.close();
    }
}
