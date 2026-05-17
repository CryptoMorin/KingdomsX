package org.kingdoms.tasks.context

import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.constants.namespace.Namespaced
import org.kingdoms.constants.namespace.NamespacedMap
import org.kingdoms.tasks.Task
import org.kingdoms.tasks.TaskState
import org.kingdoms.tasks.container.TaskSession
import org.kingdoms.tasks.priority.EnumPriority
import org.kingdoms.tasks.priority.Priority
import org.kingdoms.tasks.priority.PriorityPhase

interface TaskContext {
    var state: TaskState
    val session: TaskSession

    fun createNew(): TaskContext
}

interface InputTaskContext<I> : TaskContext {
    val input: I
}

interface OutputTaskContext<O> : TaskContext {
    var output: O?
}

interface IOTaskContext<I, O> : TaskContext, InputTaskContext<I>, OutputTaskContext<O> {
    val taskOverriders: NamespacedMap<Task<*>>

    fun skipTask(namespace: Namespace) {
        taskOverriders[namespace] = IdentityTask(namespace)
    }

    fun replaceTask(task: Task<*>) {
        taskOverriders[task.namespace] = task
    }

    fun processTask(task: Task<*>): Task<*> = taskOverriders[task.namespace] ?: task
}

abstract class AbstractTaskContext(override val session: TaskSession) : TaskContext {
    override var state: TaskState = TaskState.CONTINUE
}

private class IdentityTask(
    private val namespace: Namespace
) : Task<TaskContext> {
    override val priority: Priority = EnumPriority(PriorityPhase.NORMAL)
    override val parent: Task<TaskContext>? = null

    override fun run(context: TaskContext) {}
    override fun hashCode(): Int = throw UnsupportedOperationException("$this")
    override fun equals(other: Any?): Boolean = throw UnsupportedOperationException("$this")
    override fun getNamespace(): Namespace = namespace
}

open class AbstractIOTaskContext<I, O>(override val input: I, session: TaskSession) :
    AbstractTaskContext(session), IOTaskContext<I, O> {
    override var output: O? = null
    override val taskOverriders = NamespacedMap<Task<*>>()

    override fun createNew(): TaskContext = AbstractIOTaskContext<I, O>(input, session).let {
        it.state = this.state
        it
    }

    override fun toString() = "IOTaskContext($input => $output)"
}