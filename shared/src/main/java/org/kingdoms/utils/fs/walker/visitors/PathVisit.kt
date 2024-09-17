package org.kingdoms.utils.fs.walker.visitors

import java.io.IOException
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

class PathVisit internal constructor(
    val visitType: Type,
    val path: Path,
    val attributes: BasicFileAttributes?,
    val exception: IOException?
) {
    internal constructor(type: Type, file: Path, attrs: BasicFileAttributes?) : this(type, file, attrs, null)
    internal constructor(type: Type, file: Path, ioe: IOException?) : this(type, file, null, ioe)

    fun hasErrors(): Boolean = exception != null

    enum class Type {
        START_DIRECTORY,
        END_DIRECTORY,

        /**
         * An entry (file) in a directory
         */
        ENTRY
    }
}