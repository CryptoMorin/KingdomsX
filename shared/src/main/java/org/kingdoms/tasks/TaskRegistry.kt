package org.kingdoms.tasks

import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.annotations.Unmodifiable
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.constants.namespace.NamespacedRegistry
import org.kingdoms.tasks.annotations.TaskAnnotationProcessor
import org.kingdoms.tasks.annotations.TaskSessionConstructor
import org.kingdoms.tasks.container.AbstractTaskSession
import org.kingdoms.tasks.container.LocalTaskSession
import org.kingdoms.tasks.container.TaskSession
import org.kingdoms.tasks.context.AbstractIOTaskContext
import org.kingdoms.tasks.context.IOTaskContext
import org.kingdoms.tasks.context.TaskContext
import org.kingdoms.utils.internal.arrays.KotlinArrayExtensions.require
import org.kingdoms.utils.internal.reflection.Reflect
import java.lang.reflect.Constructor
import java.util.*
import java.util.function.Consumer

class TaskRegistry<C : TaskContext, T : Task<C>>(private val parentTask: ParentTask<C>? = null) :
    NamespacedRegistry<T>() {
    private val usableList: LinkedList<T> = LinkedList()
    private var needsUpdating = true

    override fun register(task: T) {
        val namespace: Namespace = task.getNamespace()
        Objects.requireNonNull(namespace, "Cannot register task with null namespace")
        if (parentTask != null && task.parent != parentTask) {
            throw IllegalArgumentException("Task parent mismatch: $task not a child of $parentTask")
        }

        val prev: T? = registry.putIfAbsent(namespace, task)
        require(prev == null) { "$task was already registered" }

        usableList.add(task)
        this.needsUpdating = true
    }

    // private val padder = StringPadder()
    //
    // fun comp(first: String, second: String) {
    //     val registry = ClaimProcessor.getTasks()
    //     val first = registry.getRegistered(Namespace.fromString(first))
    //     val second = registry.getRegistered(Namespace.fromString(second))
    //     val (c, s) = first.compareTo(second, registry)
    //
    //     padder.pad("  &8| ", "&2${first.namespace.asNormalizedString()}", " &9<=> ", "&2${second.namespace.asNormalizedString()}",
    //         " &8-> ", "&4$c&7:(&e${s.joinToString(" &7-> &e")}&7)")
    // }
    //
    // @JvmStatic fun test() {
    //     comp("WORLD_AND_PERMISSION", "CLAIMS")
    //     comp("CLAIMS", "WORLD_AND_PERMISSION")
    //     comp("WORLD_AND_PERMISSION", "MAX_CLAIMS")
    //     padder.getPadded().forEach { MessageHandler.sendConsoleMessage(it) }
    //     MessageHandler.sendConsoleMessage(
    //         ClaimProcessor.getTasks().getUsableList()
    //             .joinToString(" &7-> ") { "&6${it.namespace.asNormalizedString()}&7:&4${it.priority}" })
    // }

    private inner class TaskComparator : Comparator<Task<C>> {
        override fun compare(first: Task<C>, second: Task<C>): Int {
            return first.compareTo(second, this@TaskRegistry)
        }
    }

    fun getUsableList(): @Unmodifiable List<T> {
        if (needsUpdating) {
            this.usableList.sortWith(TaskComparator())
            this.needsUpdating = false
        }
        return Collections.unmodifiableList(this.usableList)
    }

    fun <I, O> executeTasks(input: I, onOutput: Consumer<O>): TaskContext {
        val session: TaskSession = AbstractTaskSession()
        val context: IOTaskContext<I, O> = AbstractIOTaskContext(input, session)
        executeDefinedTasks(context, onOutput)
        return context
    }

    @Suppress("UNCHECKED_CAST")
    @Internal
    fun <I, O> executeDefinedTasks(context: IOTaskContext<I, O>, onOutput: Consumer<O>) {
        for (task in getUsableList()) {
            val subContext = context.createNew() as IOTaskContext<I, O>
            try {
                task.run(subContext as C)
            } catch (ex: Throwable) {
                RuntimeException("Error while running task $task with context $subContext", ex).printStackTrace()
                subContext.state = TaskState.ERROR
            }
            context.state = subContext.state
            if (subContext.state == TaskState.MUST_STOP) return
            if (subContext.output != null) {
                context.output = subContext.output
                onOutput.accept(subContext.output as O)
            }
        }
    }

    fun register(container: Class<out LocalTaskSession>) {
        register(container, DefaultTaskSessionConstructor(container))
    }

    fun register(container: Class<out LocalTaskSession>, constructor: TaskSessionConstructor<C>) {
        val delegateConstructor = DefinedTaskSessionConstructor(constructor)
        val tasks: List<Task<C>> = TaskAnnotationProcessor(container, delegateConstructor).generate()
        tasks.forEach { register(it as T) }
    }
}

class DefinedTaskSessionConstructor<C : TaskContext>(private val delegate: TaskSessionConstructor<C>) :
    TaskSessionConstructor<C> {
    override fun createSession(context: C): LocalTaskSession {
        val instance = delegate.createSession(context)
        context.session.instances[instance.javaClass] = instance
        return instance
    }
}

class DefaultTaskSessionConstructor<C : TaskContext>(clazz: Class<out LocalTaskSession>) :
    TaskSessionConstructor<C> {
    val hierarchy: Array<Constructor<*>>

    init {
        val classHierarchy: Array<Class<*>> = Reflect.getClassHierarchy(clazz, true)
            .require({ LocalTaskSession::class.java.isAssignableFrom(it) }) { "All classes must be a LocalTaskSession, $it was not." }

        val constructorHierarchy: MutableList<Constructor<*>> = mutableListOf()
        var lastEnclosingClass: Class<*>? = null
        for (clazz in classHierarchy) {
            val ctor = if (lastEnclosingClass == null) {
                // First most outermost class doesn't have an enclosing class.
                clazz.getConstructor()
            } else {
                clazz.getConstructor(lastEnclosingClass)
            }
            lastEnclosingClass = clazz
        }

        this.hierarchy = constructorHierarchy.toTypedArray()
    }

    @Suppress("UNCHECKED_CAST")
    override fun createSession(context: C): LocalTaskSession {
        var lastInstance: LocalTaskSession? = null
        var enclosing: Class<*>? = null

        try {
            for (constructor in hierarchy) {
                val clazz = constructor.declaringClass
                var instance: LocalTaskSession? = context.session.instances[clazz]
                if (instance == null) {
                    instance = if (enclosing == null) {
                        if (hierarchy.size > 1) throw RuntimeException("The enclosing ($constructor) instance was not found")
                        constructor.newInstance() as LocalTaskSession
                    } else {
                        // Cannot read field "ignoreAdmin" because "x0" is null
                        // Cannot read field "ignoreAdmin" because "this.this$0" is null
                        // These are because the enclosing class instance given here is null.
                        constructor.newInstance(lastInstance!!) as LocalTaskSession
                    }
                    context.session.instances[clazz as Class<LocalTaskSession>] = instance
                }
                lastInstance = instance
                enclosing = clazz
            }
        } catch (ex: Throwable) {
            throw RuntimeException(
                "Failed to create session for ${hierarchy.contentToString()} $enclosing | $lastInstance",
                ex
            )
        }

        return lastInstance!!
    }
}