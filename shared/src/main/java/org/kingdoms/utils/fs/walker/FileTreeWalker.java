package org.kingdoms.utils.fs.walker;

import org.kingdoms.utils.fs.walker.visitors.PathVisit;
import org.kingdoms.utils.fs.walker.visitors.PathVisit.Type;
import org.kingdoms.utils.fs.walker.visitors.PathVisitor;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * Walks a file tree, generating a sequence of events corresponding to the files
 * in the tree.
 */
public class FileTreeWalker implements Closeable, FileWalkerController {
    private final boolean followLinks;
    private final LinkOption[] linkOptions;
    private final int maxDepth;
    private final ArrayDeque<DirectoryNode> stack = new ArrayDeque<>();
    private boolean closed;

    public static Stream<PathVisit> walk(Path start,
                                         Set<FileVisitOption> options,
                                         int maxDepth,
                                         AtomicReference<FileWalkerController> controller) throws IOException {
        FileTreeIterator iterator = new FileTreeIterator(start, maxDepth, options.toArray(new FileVisitOption[0]));
        controller.set(iterator);
        try {
            Spliterator<PathVisit> spliterator =
                    Spliterators.spliteratorUnknownSize(iterator, Spliterator.DISTINCT);
            return StreamSupport.stream(spliterator, false)
                    .onClose(iterator::close);
        } catch (Error | RuntimeException ex) {
            iterator.close();
            throw ex;
        }
    }

    public static void walkFileTree(Path start,
                                    Set<FileVisitOption> options,
                                    int maxDepth,
                                    PathVisitor visitor) {
        try (FileTreeWalker walker = new FileTreeWalker(options, maxDepth)) {
            PathVisit ev = walker.walk(start);
            do {
                FileVisitResult result;
                switch (ev.getVisitType()) {
                    case ENTRY: {
                        result = visitor.onVisit(ev);
                        break;
                    }
                    case START_DIRECTORY: {
                        // FileVisitResult res = visitor.preVisitDirectory(ev.file, ev.attrs);
                        FileVisitResult res = visitor.onVisit(ev);

                        // if SKIP_SIBLINGS and SKIP_SUBTREE is returned then
                        // there shouldn't be any more events for the current
                        // directory.
                        if (res == FileVisitResult.SKIP_SUBTREE || res == FileVisitResult.SKIP_SIBLINGS) {
                            walker.skipDirectory();
                        }
                        result = res;
                        break;
                    }
                    case END_DIRECTORY: {
                        FileVisitResult res = visitor.onVisit(ev);

                        // SKIP_SIBLINGS is a no-op for postVisitDirectory
                        if (res == FileVisitResult.SKIP_SIBLINGS) {
                            res = FileVisitResult.CONTINUE;
                        }
                        result = res;
                        break;
                    }
                    default:
                        throw new AssertionError("Should not get here: " + ev.getVisitType());
                }

                if (Objects.requireNonNull(result) != FileVisitResult.CONTINUE) {
                    if (result == FileVisitResult.TERMINATE) {
                        break;
                    } else if (result == FileVisitResult.SKIP_SIBLINGS) {
                        walker.skipRemainingSiblings();
                    }
                }
                ev = walker.next();
            } while (ev != null);
        }
    }

    /**
     * The element on the walking stack corresponding to a directory node.
     */
    private static class DirectoryNode {
        private final Path dir;
        private final Object key;
        private final DirectoryStream<Path> stream;
        private final Iterator<Path> iterator;
        private boolean skipped;

        DirectoryNode(Path dir, Object key, DirectoryStream<Path> stream) {
            this.dir = dir;
            this.key = key;
            this.stream = stream;
            this.iterator = stream.iterator();
        }

        void skip() {
            skipped = true;
        }
    }

    /**
     * Creates a {@code FileTreeWalker}.
     *
     * @throws IllegalArgumentException
     *          if {@code maxDepth} is negative
     * @throws ClassCastException
     *          if {@code options} contains an element that is not a
     *          {@code FileVisitOption}
     * @throws NullPointerException
     *          if {@code options} is {@code null} or the options
     *          array contains a {@code null} element
     */
    FileTreeWalker(Collection<FileVisitOption> options, int maxDepth) {
        boolean fl = false;
        for (FileVisitOption option : options) {
            // will throw NPE if options contains null
            switch (option) {
                case FOLLOW_LINKS:
                    fl = true;
                    break;
                default:
                    throw new AssertionError("Should not get here");
            }
        }
        if (maxDepth < 0)
            throw new IllegalArgumentException("'maxDepth' is negative");

        this.followLinks = fl;
        this.linkOptions = (fl) ? new LinkOption[0] : new LinkOption[]{LinkOption.NOFOLLOW_LINKS};
        this.maxDepth = maxDepth;
    }

    /**
     * Returns the attributes of the given file, taking into account whether
     * the walk is following sym links is not. The {@code canUseCached}
     * argument determines whether this method can use cached attributes.
     */
    private BasicFileAttributes getAttributes(Path file) throws IOException {
        // attempt to get attributes of file. If fails and we are following
        // links then a link target might not exist so get attributes of link
        BasicFileAttributes attrs;
        try {
            attrs = Files.readAttributes(file, BasicFileAttributes.class, linkOptions);
        } catch (IOException ioe) {
            if (!followLinks) throw ioe;

            // attempt to get attrmptes without following links
            attrs = Files.readAttributes(file,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS
            );
        }
        return attrs;
    }

    /**
     * Returns true if walking into the given directory would result in a
     * file system loop/cycle.
     */
    private boolean wouldLoop(Path dir, Object key) {
        // if this directory and ancestor has a file key then we compare
        // them; otherwise we use less efficient isSameFile test.
        for (DirectoryNode ancestor : stack) {
            Object ancestorKey = ancestor.key;
            if (key != null && ancestorKey != null) {
                if (key.equals(ancestorKey)) {
                    return true; // Cycle detected
                }
            } else {
                try {
                    if (Files.isSameFile(dir, ancestor.dir)) {
                        return true; // Cycle detected
                    }
                } catch (IOException | SecurityException ignored) {
                }
            }
        }
        return false;
    }

    /**
     * Visits the given file, returning the {@code Event} corresponding to that
     * visit.
     *
     * The {@code ignoreSecurityException} parameter determines whether
     * any SecurityException should be ignored or not. If a SecurityException
     * is thrown, and is ignored, then this method returns {@code null} to
     * mean that there is no event corresponding to a visit to the file.
     *
     * The {@code canUseCached} parameter determines whether cached attributes
     * for the file can be used or not.
     */
    private PathVisit visit(Path entry, boolean ignoreSecurityException, boolean canUseCached) {
        // need the file attributes
        BasicFileAttributes attrs;
        try {
            attrs = getAttributes(entry);
        } catch (IOException ioe) {
            return new PathVisit(Type.ENTRY, entry, ioe);
        } catch (SecurityException se) {
            if (ignoreSecurityException)
                return null;
            throw se;
        }

        // at maximum depth or file is not a directory
        int depth = stack.size();
        if (depth >= maxDepth || !attrs.isDirectory()) {
            return new PathVisit(Type.ENTRY, entry, attrs);
        }

        // check for cycles when following links
        if (followLinks && wouldLoop(entry, attrs.fileKey())) {
            return new PathVisit(Type.ENTRY, entry,
                    new FileSystemLoopException(entry.toString()));
        }

        // file is a directory, attempt to open it
        DirectoryStream<Path> stream = null;
        try {
            stream = Files.newDirectoryStream(entry);
        } catch (IOException ioe) {
            return new PathVisit(Type.ENTRY, entry, ioe);
        } catch (SecurityException se) {
            if (ignoreSecurityException)
                return null;
            throw se;
        }

        // push a directory node to the stack and return an event
        stack.push(new DirectoryNode(entry, attrs.fileKey(), stream));
        return new PathVisit(Type.START_DIRECTORY, entry, attrs);
    }

    /**
     * Start walking from the given file.
     */
    PathVisit walk(Path file) {
        if (closed)
            throw new IllegalStateException("Closed");

        PathVisit ev = visit(file,
                false,   // ignoreSecurityException
                false);  // canUseCached
        assert ev != null;
        return ev;
    }

    /**
     * Returns the next Event or {@code null} if there are no more events or
     * the walker is closed.
     */
    PathVisit next() {
        DirectoryNode top = stack.peek();
        if (top == null)
            return null;      // stack is empty, we are done

        // continue iteration of the directory at the top of the stack
        PathVisit ev;
        do {
            Path entry = null;
            IOException ioe = null;

            // get next entry in the directory
            if (!top.skipped) {
                Iterator<Path> iterator = top.iterator;
                try {
                    if (iterator.hasNext()) {
                        entry = iterator.next();
                    }
                } catch (DirectoryIteratorException x) {
                    ioe = x.getCause();
                }
            }

            // no next entry so close and pop directory,
            // creating corresponding event
            if (entry == null) {
                try {
                    top.stream.close();
                } catch (IOException e) {
                    if (ioe == null) {
                        ioe = e;
                    } else {
                        ioe.addSuppressed(e);
                    }
                }
                stack.pop();
                return new PathVisit(Type.END_DIRECTORY, top.dir, ioe);
            }

            // visit the entry
            ev = visit(entry,
                    true,   // ignoreSecurityException
                    true);  // canUseCached

        } while (ev == null);

        return ev;
    }

    @Override
    public void skipDirectory() {
        if (!stack.isEmpty()) {
            DirectoryNode node = stack.pop();
            try {
                node.stream.close();
            } catch (IOException ignore) {
            }
        }
    }

    @Override
    public void skipRemainingSiblings() {
        if (!stack.isEmpty()) {
            stack.peek().skip();
        }
    }

    public boolean isOpen() {
        return !closed;
    }

    /**
     * Closes/pops all directories on the stack.
     */
    @Override
    public void close() {
        if (!closed) {
            while (!stack.isEmpty()) skipDirectory();
            closed = true;
        }
    }
}
