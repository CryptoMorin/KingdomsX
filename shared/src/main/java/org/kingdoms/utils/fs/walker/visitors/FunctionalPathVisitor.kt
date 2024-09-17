package org.kingdoms.utils.fs.walker.visitors

import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.util.function.Consumer
import java.util.function.Predicate

class FunctionalPathVisitor(val root: Path) : PathVisitor {
    var visitors: MutableList<ConditionalPathVisitor> = arrayListOf()

    private fun getPathPredicate(folder: Boolean, resolvablePath: Path) =
        if (folder) StartsWithPathFilter(resolvablePath)
        else ExactPathFilter(resolvablePath)

    private fun stringVisitor(folder: Boolean, resolvablePath: String, visit: Boolean): FunctionalPathVisitor {
        var exactPath: Path = root
        resolvablePath.split('/').forEach { exactPath = root.resolve(it) }
        return visitor(folder, getPathPredicate(folder, exactPath), visit)
    }

    private fun pathVisitor(folder: Boolean, path: Path, visit: Boolean): FunctionalPathVisitor {
        require(path.startsWith(root)) { "Given path '$path' isn't included in the root path '$root'" }
        return visitor(folder, getPathPredicate(folder, path), visit)
    }

    private fun visitor(folder: Boolean, filter: Predicate<Path>, visit: Boolean): FunctionalPathVisitor {
        val handle = ConditionalPathVisitor(filter, if (visit) VisitAll else SkipAll)
        visitors.add(handle)
        return this
    }

    fun onlyIf(condition: Boolean, handler: Consumer<FunctionalPathVisitor>): FunctionalPathVisitor {
        if (condition) handler.accept(this)
        return this
    }

    fun visitFiles(filter: Predicate<Path>): FunctionalPathVisitor = visitor(false, filter, true)
    fun skipFiles(filter: Predicate<Path>): FunctionalPathVisitor = visitor(false, filter, true)

    fun visitFolders(filter: Predicate<Path>): FunctionalPathVisitor = visitor(true, filter, true)
    fun skipFolders(filter: Predicate<Path>): FunctionalPathVisitor = visitor(true, filter, true)


    fun visitFile(resolvablePath: String): FunctionalPathVisitor = stringVisitor(false, resolvablePath, true)
    fun skipFile(resolvablePath: String): FunctionalPathVisitor = stringVisitor(false, resolvablePath, false)

    fun visitFolder(resolvablePath: String): FunctionalPathVisitor = stringVisitor(true, resolvablePath, true)
    fun skipFolder(resolvablePath: String): FunctionalPathVisitor = stringVisitor(true, resolvablePath, false)

    fun visitFile(path: Path): FunctionalPathVisitor = pathVisitor(false, path, true)
    fun skipFile(path: Path): FunctionalPathVisitor = pathVisitor(false, path, false)

    fun visitFolder(path: Path): FunctionalPathVisitor = pathVisitor(true, path, true)
    fun skipFolder(path: Path): FunctionalPathVisitor = pathVisitor(true, path, false)

    override fun onVisit(visit: PathVisit): FileVisitResult {
        if (visit.path == root) return FileVisitResult.CONTINUE

        for (pathVisitHandle in visitors) {
            if (pathVisitHandle.predicate.test(visit.path)) {
                return pathVisitHandle.visitor.onVisit(visit)
            }
        }

        return FileVisitResult.SKIP_SUBTREE
    }
}