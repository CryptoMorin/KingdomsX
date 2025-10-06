package org.kingdoms.utils.internal.string

import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.utils.internal.functional.Fn
import java.util.*

fun interface PrettyString<T> {
    fun toPrettyString(obj: T, context: PrettyStringContext)
}

class PrettyStringContext(val string: StringBuilder, val nestLevel: Int) {
    val line: Int get() = string.toString().split('\n').count()
    val column: Int get() = string.toString().substringAfterLast('\n').length

    fun delegate(obj: Any?, direct: Boolean) {
        if (obj == null) {
            string.append("null")
            return
        }
        val specialized = ObjectPrettyStringFactory.findSpecialized(obj::class.java, direct)
        if (specialized != null) {
            val newContext = PrettyStringContext(string, nestLevel + 1)
            specialized.toPrettyString(Fn.cast(obj), newContext)
        } else {
            val space = " ".repeat(((nestLevel + 1) * 2))
            string.append(obj.toString()).split('\n').joinToString { space + "\n" }
        }
    }
}

object ObjectPrettyStringFactory {
    @JvmStatic val REGISTRY: MutableMap<Class<*>, PrettyString<*>> = IdentityHashMap()

    @JvmStatic fun findSpecialized(clazz: Class<*>?, direct: Boolean): PrettyString<*>? {
        if (clazz == null || clazz == Object::class.java) return null
        val directType = REGISTRY[clazz]
        if (direct) return directType

        return directType ?: findSpecialized(clazz.superclass, true)
        ?: clazz.interfaces.firstNotNullOfOrNull { findSpecialized(it, true) }
    }

    init {
        REGISTRY[Map::class.java] = PrettyString<Map<Any, Any>> { obj, context ->
            associatedArrayMap(obj, context)
        }
        REGISTRY[Namespace::class.java] = PrettyString<Namespace> { obj, context ->
            context.string.append(obj.asString())
        }
        REGISTRY[Collection::class.java] = PrettyString<Collection<Any>> { obj, context ->
            context.string.append(obj.javaClass.simpleName).append('(').append(obj.joinToString(", ")).append(')')
        }
    }

    @JvmStatic fun <K, V> associatedArrayMap(map: Map<K, V>, context: PrettyStringContext) {
        val builder = context.string
        val nestLevel = context.nestLevel
        builder.append(map.javaClass.simpleName).append('(')

        for ((key, value) in map.entries) {
            builder.append('\n')
            builder.append(" ".repeat((nestLevel * 2)))
            builder.append('(').append(nestLevel).append(')')
            context.delegate(key, true)
            builder.append(" => ")
            context.delegate(value, true)
            builder.append('\n')
        }

        builder.append(')')
        if (nestLevel > 1) builder.append('\n')
    }

    @JvmStatic fun toDefaultPrettyString(any: Any?): String = any.toPrettyString()
}

fun Any?.toPrettyString(): String {
    val context = PrettyStringContext(StringBuilder(), 0)
    context.delegate(this, false)
    return context.string.toString()
}