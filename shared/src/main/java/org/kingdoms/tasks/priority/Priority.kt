package org.kingdoms.tasks.priority

import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.tasks.Task
import org.kingdoms.tasks.TaskRegistry
import org.kingdoms.tasks.priority.Priority.Companion.UNSUPPORTED_COMPARABLE

interface Priority {
    companion object {
        const val UNSUPPORTED_COMPARABLE = Int.MAX_VALUE
    }

    fun compareTo(other: Task<*>, registry: TaskRegistry<*, *>): Int
    fun validateFor(task: Task<*>): String? = null
}

enum class PriorityPhase { LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR }

open class EnumPriority(val type: PriorityPhase) : NumberedPriority(type.ordinal) {
    override fun toString() = "EnumPriority($type)"
}

open class NumberedPriority(val order: Int) : Priority {
    override fun compareTo(other: Task<*>, registry: TaskRegistry<*, *>): Int {
        return when (val priority = other.priority) {
            is NumberedPriority -> this.order.compareTo(priority.order)
            else -> UNSUPPORTED_COMPARABLE
        }
    }

    override fun toString() = "NumberedPriority($order)"
}

open class RelativePriority(val type: Type, val targetTaskNS: Namespace) : Priority {
    enum class Type { BEFORE, AFTER, REPLACE }

    override fun validateFor(task: Task<*>): String? {
        return if (task.namespace == targetTaskNS) "Circular task priority" else null
    }

    override fun compareTo(second: Task<*>, registry: TaskRegistry<*, *>): Int {
        return if (second.namespace == targetTaskNS) {
            when (this.type) {
                Type.BEFORE -> -1
                Type.REPLACE -> 0
                Type.AFTER -> 1
            }
        } else {
            val registeredTask = registry.getRegistered(targetTaskNS)
            if (registeredTask != null) {
                // A after B -> C after B -> B
                // A comparedTo C => B comparedTo C => 1
                val compared = registeredTask.compareTo(second, registry)
                if (compared == UNSUPPORTED_COMPARABLE) {
                    // p = "S"
                    UNSUPPORTED_COMPARABLE
                } else {
                    // p = "B"
                    when (this.type) {
                        // This wanted to run before the other task
                        Type.BEFORE -> when {
                            compared < 0 -> -1 // The BEFORE task also wants to run before the target task.
                            compared > 0 -> +1 // The BEFORE task wants to run after target task.
                            else -> 0
                        }

                        Type.REPLACE -> 0

                        Type.AFTER -> when {
                            compared < 0 -> -1 // The AFTER task wants to run before target task.
                            compared > 0 -> +1 // The AFTER task also wants to run after the target task.
                            else -> 0
                        }
                    }
                }
            } else {
                // p = "C"
                0
            }
        }
        // return Pair(a, other.plus("$p$a"))
    }

    override fun toString() = "RelativePriority($type ${targetTaskNS.asString()})"
}