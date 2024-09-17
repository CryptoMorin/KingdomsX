package org.kingdoms.utils.fs.walker

import java.nio.file.FileVisitResult

interface FileWalkerController {
    /**
     * Pops the directory node that is the current top of the stack so that
     * there are no more events for the directory (including no END_DIRECTORY)
     * event. This method is a no-op if the stack is empty or the walker is
     * closed.
     */
    fun skipDirectory()

    /**
     * Skips the remaining entries in the directory at the top of the stack.
     * This method is a no-op if the stack is empty or the walker is closed.
     */
    fun skipRemainingSiblings()

    fun close()

    fun processResult(result: FileVisitResult) = when (result) {
        FileVisitResult.TERMINATE -> close()
        FileVisitResult.SKIP_SUBTREE -> skipDirectory()
        FileVisitResult.SKIP_SIBLINGS -> skipRemainingSiblings()
        FileVisitResult.CONTINUE -> {}
    }
}