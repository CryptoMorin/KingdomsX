package org.kingdoms.tasks.container

import org.kingdoms.constants.namespace.NamespacedMap
import org.kingdoms.tasks.context.TaskContext
import java.util.*

interface TaskSession {
    val results: NamespacedMap<TaskContext>
    val instances: MutableMap<Class<out LocalTaskSession>, LocalTaskSession>
}

class AbstractTaskSession : TaskSession {
    override val results = NamespacedMap<TaskContext>()
    override val instances: MutableMap<Class<out LocalTaskSession>, LocalTaskSession> = IdentityHashMap()
}

interface LocalTaskSession {
    fun close() {}
}

interface ConditionalLocalTaskSession<C : TaskContext> : LocalTaskSession {
    fun shouldExecute(context: C): Boolean
}