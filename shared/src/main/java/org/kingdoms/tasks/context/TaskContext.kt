package org.kingdoms.tasks.context

import org.kingdoms.tasks.TaskState
import org.kingdoms.tasks.container.TaskSession

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

interface IOTaskContext<I, O> : TaskContext, InputTaskContext<I>, OutputTaskContext<O>

abstract class AbstractTaskContext(override val session: TaskSession) : TaskContext {
    override var state: TaskState = TaskState.CONTINUE
}

open class AbstractIOTaskContext<I, O>(override val input: I, session: TaskSession) :
    AbstractTaskContext(session), IOTaskContext<I, O> {
    override var output: O? = null

    override fun createNew(): TaskContext = AbstractIOTaskContext<I, O>(input, session).let {
        it.state = this.state
        it
    }

    override fun toString() = "IOTaskContext($input => $output)"
}