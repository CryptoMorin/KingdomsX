package org.kingdoms.ide;

/**
 * Used only for {@link Bookmark}.
 */
public enum BookmarkType {
    /**
     * A {@code TODO} that is expected to be handled
     * in the near future. Another alternative is {@code KLogger.todo()}
     * which is kept during compilation of course.
     */
    REMINDER,

    /**
     * Indicates that the code is kept for archive and study purposes
     * and should not be used within the project.
     */
    ARCHIVE,

    /**
     * Indicates that the code needs to be finished later.
     * A more long-term version of {@link #REMINDER}.
     */
    UNFINISHED,

    /**
     * Having doubts about whether the code, usually a special fix, is actually fixing
     * something or working as intended or not.
     */
    QUESTIONABLE,

    /**
     * The code is messy and could be improved by changing
     * its structure.
     */
    CLEANUP,

    /**
     * Indicates that the code is relatively new and subject to
     * a lot of changes in the future.
     */
    EXPERIMENTAL,

    /**
     * Indicates that the code can suffer from poor performance.
     */
    PERFORMANCE,

    /**
     * Indicates that the code is considered dangerous and care
     * should be taken when modifying it, or it should be entirely
     * replaced by a safer code in the future.
     * It can also indicate that this object is not safe to access
     * for normal usages.
     */
    UNSAFE,

    /**
     * A special type of unsafe code which has the potential to be
     * abused by players in the server.
     */
    EXPLOITABLE;
}
