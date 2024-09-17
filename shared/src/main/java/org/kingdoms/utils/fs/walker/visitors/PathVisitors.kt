package org.kingdoms.utils.fs.walker.visitors

import org.kingdoms.utils.fs.walker.FileTreeWalker
import org.kingdoms.utils.fs.walker.FileWalkerController
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Predicate
import java.util.stream.Stream

class ConditionalPathVisitor(val predicate: Predicate<Path>, val visitor: PathVisitor) {
    override fun toString(): String = "ConditionalPathVisitor($predicate -> $visitor)"
}

class ExactPathFilter(val exactPath: Path) : Predicate<Path> {
    override fun test(path: Path): Boolean = exactPath == path

    override fun toString(): String = "ExactPathFilter($exactPath)"
}

class StartsWithPathFilter(val startsWith: Path) : Predicate<Path> {
    override fun test(path: Path): Boolean = path.startsWith(startsWith)

    override fun toString(): String = "StartsWithPathFilter($startsWith)"
}

interface PathVisitor {
    fun onVisit(visit: PathVisit): FileVisitResult
    fun collect(root: Path) = CollectorPathVisitor(root, this).getFiles()
    fun stream(root: Path): Stream<PathVisit> {
        val controller = AtomicReference<FileWalkerController>()
        return FileTreeWalker.walk(root, hashSetOf(), Integer.MAX_VALUE, controller).filter {
            val result = onVisit(it)
            controller.get().processResult(result)
            when (result) {
                FileVisitResult.CONTINUE, FileVisitResult.SKIP_SIBLINGS -> true
                else -> false
            }
        }
    }
}

class CollectorPathVisitor(val root: Path, val visitor: PathVisitor) : PathVisitor {
    val list: MutableList<Path> = arrayListOf()

    override fun onVisit(visit: PathVisit): FileVisitResult {
        val result = visitor.onVisit(visit)
        when (result) {
            FileVisitResult.CONTINUE, FileVisitResult.SKIP_SIBLINGS -> list.add(visit.path)
            else -> {}
        }
        return result
    }

    fun getFiles(): List<Path> {
        FileTreeWalker.walkFileTree(root, hashSetOf(), Integer.MAX_VALUE, this)
        return list
    }
}

object VisitAll : PathVisitor {
    override fun onVisit(visit: PathVisit) = FileVisitResult.CONTINUE
    override fun toString(): String = "PathVisitor:VisitAll"
}

object SkipAll : PathVisitor {
    override fun onVisit(visit: PathVisit) = FileVisitResult.SKIP_SUBTREE
    override fun toString(): String = "PathVisitor:SkipAll"
}
