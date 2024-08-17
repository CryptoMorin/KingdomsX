package org.kingdoms.utils

import org.kingdoms.utils.internal.nonnull.NonNullMap
import org.kingdoms.utils.internal.reflection.ClassHierarchyWalker
import java.util.*
import java.util.function.Function

class HierarchalClassMap<V : Any>(
    private val original: MutableMap<Class<*>, Optional<V>> = NonNullMap.of(IdentityHashMap())
) :
    MutableMap<Class<*>, V> {

    private val hierarchyWalker = HierarchyWalker()

    private inner class HierarchyWalker : Function<Class<*>, Optional<V>?> {
        override fun apply(clazz: Class<*>): Optional<V>? = original[clazz]
    }

    override fun get(key: Class<*>): V? {
        var found: Optional<V>? = original[key]

        if (found === null) {
            found = ClassHierarchyWalker.walk(key, hierarchyWalker)
            if (found === null) found = Optional.empty()

            original[key] = found
        }

        return found.orElse(null)
    }

    override fun containsKey(key: Class<*>): Boolean = get(key) != null

    override val entries: MutableSet<MutableMap.MutableEntry<Class<*>, V>>
        get() = TODO("Not yet implemented")
    override val keys: MutableSet<Class<*>>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: MutableCollection<V>
        get() = TODO("Not yet implemented")

    override fun clear() {
        original.clear()
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun remove(key: Class<*>): V? {
        TODO("Not yet implemented")
    }

    override fun putAll(from: Map<out Class<*>, V>) {
        from.forEach { put(it.key, it.value) }
    }

    override fun put(key: Class<*>, value: V): V? =
        original.put(key, Optional.of(value))?.orElse(null)

    override fun containsValue(value: V): Boolean {
        TODO("Not yet implemented")
    }
}
