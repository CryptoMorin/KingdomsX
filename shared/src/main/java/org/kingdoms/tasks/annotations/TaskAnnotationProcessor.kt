package org.kingdoms.tasks.annotations

import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.tasks.*
import org.kingdoms.tasks.Task
import org.kingdoms.tasks.container.ConditionalLocalTaskSession
import org.kingdoms.tasks.container.LocalTaskSession
import org.kingdoms.tasks.context.IOTaskContext
import org.kingdoms.tasks.context.InputTaskContext
import org.kingdoms.tasks.context.OutputTaskContext
import org.kingdoms.tasks.context.TaskContext
import org.kingdoms.tasks.priority.EnumPriority
import org.kingdoms.tasks.priority.Priority
import org.kingdoms.tasks.priority.PriorityPhase
import org.kingdoms.tasks.priority.RelativePriority
import org.kingdoms.utils.internal.enumeration.QuickEnumSet
import org.kingdoms.utils.internal.reflection.annotations.AnnotationContainer
import java.lang.reflect.Method

@FunctionalInterface
interface TaskSessionConstructor<C : TaskContext> {
    fun createSession(context: C): LocalTaskSession
}

class TaskAnnotationProcessor<C : TaskContext> @JvmOverloads constructor(
    private val container: Class<out LocalTaskSession>,
    private val constructor: TaskSessionConstructor<C>,
    private val parentTask: ParentTask<C>? = null,
) {
    private inner class ProcessedTaskAnnotations(
        val implicitReturnState: TaskState,
        val taskStatesInclude: Boolean,
        val taskStates: MutableSet<TaskState>?,
        val priority: Priority,
        val namespace: Namespace,
    )

    private fun processAnnotation(container: AnnotationContainer): ProcessedTaskAnnotations? {
        val task = container.getAnnotation(org.kingdoms.tasks.annotations.Task::class.java) ?: return null
        val grouped = AnnotationContainer.of(container.declaringClass).getAnnotation(GroupedTask::class.java)
        val groupedNs = if (grouped == null) "" else grouped.namespace + ":"
        val namespace = Namespace.fromString(groupedNs + task.key)

        // TODO Add support for @TaskOutput annotaiton with multiple parameters.
        if (container is AnnotationContainer.Executable) {
            if (container.javaObject.parameterCount > 1) throw IllegalStateException(
                "Task has more than one parameter: ${container.javaObject}"
            )
        }

        var taskStatesInclude: Boolean = true
        var taskStates: MutableSet<TaskState>? = null
        container.getAnnotation(AcceptedTaskStates::class.java)?.let {
            taskStatesInclude = it.include
            taskStates = QuickEnumSet(TaskState.values())
            taskStates!!.addAll(it.states)
        }

        val implicitReturnState: TaskState =
            container.getAnnotation(ReturnTaskState::class.java)?.state ?: TaskState.CONTINUE

        val priorityAnnotations: List<Annotation> = container.findAll(
            org.kingdoms.tasks.annotations.Priority::class.java,
            NumberedPriority::class.java,
            Before::class.java, After::class.java, Replace::class.java
        )
        if (priorityAnnotations.size > 1) throw IllegalStateException(
            "Task cannot have more than one priority annotation. Found $priorityAnnotations for $container"
        )

        val priority: Priority = when (val priorityAnn = priorityAnnotations.first()) {
            is org.kingdoms.tasks.annotations.Priority -> EnumPriority(priorityAnn.priority)
            is NumberedPriority -> org.kingdoms.tasks.priority.NumberedPriority(priorityAnn.order)
            is Before -> RelativePriority(RelativePriority.Type.BEFORE, Namespace.fromString(priorityAnn.other))
            is After -> RelativePriority(RelativePriority.Type.AFTER, Namespace.fromString(priorityAnn.other))
            is Replace -> RelativePriority(RelativePriority.Type.REPLACE, Namespace.fromString(priorityAnn.other))
            else -> EnumPriority(PriorityPhase.NORMAL)
        }

        return ProcessedTaskAnnotations(
            implicitReturnState,
            taskStatesInclude, taskStates,
            priority,
            namespace,
        )
    }

    fun generate(): List<Task<C>> {
        val tasks: MutableList<Task<C>> = mutableListOf()
        for (method in container.declaredMethods) {
            method.isAccessible = true
            val settings = processAnnotation(AnnotationContainer.of(method))
            if (settings != null) tasks.add(ReflectionTask(constructor, settings, method, parentTask))
        }
        for (innerClass in container.declaredClasses) {
            if (!LocalTaskSession::class.java.isAssignableFrom(innerClass)) continue
            if (!ConditionalLocalTaskSession::class.java.isAssignableFrom(innerClass)) {
                throw RuntimeException("Only ConditionalLocalTaskSession are supported for parent tasks: $innerClass")
            }
            val settings = processAnnotation(AnnotationContainer.of(innerClass))
            if (settings != null) {
                val genericClass = innerClass as Class<ConditionalLocalTaskSession<C>>
                val constructor = DefaultTaskSessionConstructor<C>(genericClass)
                val parentTask = ReflectionParentTask(constructor, settings, parentTask)
                val innerProcessor = TaskAnnotationProcessor(genericClass, constructor, parentTask)
                val subTasks = innerProcessor.generate()
                subTasks.forEach { parentTask.subTasks.register(it) }
                tasks.add(parentTask)
            }
        }
        return tasks
    }

    private class ReflectionParentTask<T : TaskContext>(
        private val constructor: TaskSessionConstructor<T>,
        private val settings: TaskAnnotationProcessor<T>.ProcessedTaskAnnotations,
        parentTask: ParentTask<T>?
    ) : AbstractParentTask<T>(settings.priority, settings.namespace, parentTask) {
        @Suppress("UNCHECKED_CAST")
        override fun run(context: T) {
            if (settings.taskStates == null) {
                if (context.state == TaskState.SHOULD_STOP || context.state == TaskState.MUST_STOP) return
            } else if (settings.taskStatesInclude != settings.taskStates.contains(context.state)) return

            val instance = constructor.createSession(context) as ConditionalLocalTaskSession<T>
            if (!instance.shouldExecute(context)) return

            var lastOutput: Any? = null
            subTasks.executeDefinedTasks(context as IOTaskContext<Any, Any>) { lastOutput = it }

            if (lastOutput != null) {
                if (context is OutputTaskContext<*>) {
                    (context as OutputTaskContext<Any>).output = lastOutput
                }
            }
        }
    }

    private class ReflectionTask<T : TaskContext>(
        private val constructor: TaskSessionConstructor<T>,
        private val settings: TaskAnnotationProcessor<T>.ProcessedTaskAnnotations,
        private val method: Method,
        parentTask: ParentTask<T>?
    ) : AbstractTask<T>(settings.priority, settings.namespace, parentTask) {
        private val needsArg: Boolean = method.parameterCount != 0
        private val directInputArg: Boolean =
            needsArg && !TaskContext::class.java.isAssignableFrom(method.parameterTypes[0])
        private val hasReturnValue: Boolean = method.returnType != Void::class.java

        @Suppress("UNCHECKED_CAST")
        override fun run(context: T) {
            if (settings.taskStates == null) {
                if (context.state.shouldStop()) return
            } else if (settings.taskStatesInclude != settings.taskStates.contains(context.state)) return

            val instance = constructor.createSession(context)
            val result: Any? = if (needsArg) {
                if (directInputArg) method.invoke(instance, (context as InputTaskContext<Any>).input)
                else method.invoke(instance, context)
            } else {
                method.invoke(instance)
            }

            if (result != null) {
                if (context is OutputTaskContext<*>) {
                    (context as OutputTaskContext<Any>).output = result
                }
            }

            if (hasReturnValue && result != null) {
                context.state = settings.implicitReturnState
            }
        }
    }
}