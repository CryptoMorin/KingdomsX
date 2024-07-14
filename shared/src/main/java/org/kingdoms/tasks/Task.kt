package org.kingdoms.tasks

import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.constants.namespace.Namespaced
import org.kingdoms.tasks.context.TaskContext
import org.kingdoms.tasks.priority.Priority

interface Task<C : TaskContext> : Namespaced {
    val priority: Priority
    val parent: Task<C>?

    fun run(context: C)

    // enum class Type { RUNNABLE, INPUT, OUTPUT, IO }

    override fun hashCode(): Int
    override fun equals(other: Any?): Boolean

    fun compareTo(other: Task<*>, registry: TaskRegistry<*, *>): Int {
        // var (compared, comparedStr) = this.priority.compareTo(other, registry)
        var compared = this.priority.compareTo(other, registry)
        // val lastSize = comparedStr.size
        if (compared == Priority.UNSUPPORTED_COMPARABLE) {
            compared = other.priority.compareTo(this, registry)
            // comparedStr = a.second
            if (compared == Priority.UNSUPPORTED_COMPARABLE) {
                compared = 0
            } else {
                compared = -compared
                // comparedStr = comparedStr.plusAt(lastSize - 1, "(-)")
            }
        }
        return compared
        // return Pair(compared, comparedStr)
    }
}

interface ParentTask<C : TaskContext> : Task<C> {
    val subTasks: TaskRegistry<C, Task<C>>
}

abstract class AbstractParentTask<C : TaskContext>(priority: Priority, namespace: Namespace, parent: Task<C>?) :
    ParentTask<C>,
    AbstractTask<C>(priority, namespace, parent) {
    override val subTasks = TaskRegistry(this)
}

abstract class AbstractTask<C : TaskContext>(
    final override val priority: Priority,
    private val namespace: Namespace,
    final override val parent: Task<C>?
) : Task<C> {
    init {
        @Suppress("LeakingThis")
        priority.validateFor(this)?.let { throw IllegalArgumentException("$it for task $this") }
    }

    final override fun hashCode(): Int = namespace.hashCode()
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Task<*>) return false
        return this.namespace == other.namespace
    }

    final override fun getNamespace(): Namespace = this.namespace

    override fun toString(): String =
        "${this::class.java.simpleName}(name=${namespace.asString()}, priority=$priority, parent=$parent)"
}
