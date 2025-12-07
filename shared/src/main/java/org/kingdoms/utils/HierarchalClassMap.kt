package org.kingdoms.utils

import org.kingdoms.data.Pair
import org.kingdoms.utils.internal.nonnull.NonNullMap
import org.kingdoms.utils.internal.reflection.ClassHierarchyWalker
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.collections.Map
import kotlin.collections.forEach
import kotlin.collections.get

/**
 * A map that associates a class to a value. The difference between a normal `HashMap<Class<?>, V>`
 * and this class is that this class will also perform a lookup on the target class's parents when
 * [Map.get] is used. Meaning if the parent of the target class has a value in this map, then that
 * value is returned.
 *
 * # Priority ([Map.get])
 * The fallback system order for [Map.get] is as follows:
 *   * If the target class has a direct value in this map, the operation is `O(1)`
 *   * If the target class has a superclass or interface with a value in this map, that value is used. `O(1)` for subsequent calls.
 *   * If the target class has both a superclass and an interface with a value in this map, the superclass is prioritized.
 *   * If the target class has more than one interfaces with a value in this map, the order is undefined.
 *
 * # Iterating ([Map.entries], [Map.keys] and [Map.values])
 * The iterating operations are all `O(n)` with
 * a slight overhead of filtering raw results. Meaning these methods will only return original values.
 * So if a class has a superclass/interface with a value in this map, only the superclass/interface will
 * be included in these values, not the cached class with the inferred value.
 *
 * # Updating [Map.put]
 * This operation is `O(1)` or `O(n)` depending on how many children this class has (if it's a parent class)
 *
 *   * If a class's value is updated in the map with subclasses that previously referred to the value
 *     of this class as their parent, then their values will be updated to reflect the changes.
 *   * If the class has previously referred to its parent for its value, then this class will completely
 *     dissociate from its parent and become independent with its own value.
 *
 * _**This map implementation is not thread-safe.**_
 */
class HierarchalClassMap<C, V : Any> : MutableMap<Class<out C>, V> {
    private val original: MutableMap<Class<out C>, DataContainer> = NonNullMap.of(IdentityHashMap())
    private val hierarchyWalker = HierarchyWalker()
    private val emptyContainer = DataContainer(null, null, null)

    private inner class HierarchyWalker : Function<Class<*>, DataContainer?> {
        override fun apply(clazz: Class<*>): DataContainer? = original[clazz]
    }

    private fun isNull(container: DataContainer?): Boolean =
        container === null || container === emptyContainer

    private inner class DataContainer(
        @JvmField var data: V?,
        @JvmField var derivedFrom: DataContainer?,
        @JvmField var children: MutableSet<DataContainer>?
    ) {
        fun removeChild(child: DataContainer) {
            children?.remove(child)
        }

        fun addChild(child: DataContainer) {
            if (children === null) {
                children = Collections.newSetFromMap(IdentityHashMap())
            }

            children!!.add(child)
        }

        // Don't toString() the derivedFrom and children, it will cause a StackOverflowError
        override fun toString(): String = javaClass.simpleName +
                "#${hashCode()}(data=$data, derivedFrom=${derivedFrom !== null}, children:${children?.size})"
    }

    private fun linkRecursively(parent: DataContainer, child: Class<out C>): DataContainer {
        var childData = original[child]
        if (!isNull(childData))
            throw IllegalStateException("Child is not null $parent -> $child ($childData)")

        childData = DataContainer(parent.data, parent, null)
        original.put(child, childData)
        parent.addChild(childData)
        return childData
    }

    override fun get(key: Class<out C>): V? {
        // We need to synchronize the entire thing because the values might be being computed.
        // But we will still try and get it without a lock because in most cases once the server
        // has passed the first iterations of adding the hierarchy, this hashmap will rarely change again.
        var found: DataContainer? = original[key]
        if (found !== null) return found.data

        synchronized(original) {
            found = original[key]

            if (found === null) {
                val hierarchalData = ClassHierarchyWalker.walk(key, hierarchyWalker)
                if (hierarchalData !== null) {
                    val iter = hierarchalData.walkedPath.iterator()
                    var lastParent: DataContainer = original[iter.next()]!!

                    while (iter.hasNext()) {
                        val child: Class<*> = iter.next()
                        @Suppress("UNCHECKED_CAST")
                        lastParent = linkRecursively(lastParent, child as Class<out C>)
                    }

                    found = lastParent
                    original[key] = found
                } else {
                    // If this class and even its parents aren't in this map, cache null results
                    original[key] = emptyContainer
                    return null
                }
            }

            return found.data
        }
    }

    override fun containsKey(key: Class<out C>): Boolean = get(key) !== null

    override val entries: MutableSet<MutableMap.MutableEntry<Class<out C>, V>>
        get() = original.entries.stream()
            .filter { it.value.data !== null }
            .filter { it.value.derivedFrom === null }
            .map { Pair.of(it.key, it.value.data!!) }
            .collect(Collectors.toSet())

    override val keys: MutableSet<Class<out C>>
        get() = original.entries.stream()
            .filter { it.value.data !== null }
            .filter { it.value.derivedFrom === null }
            .map { it.key }
            .collect(Collectors.toSet())
    override val size: Int
        get() = original.values.stream()
            .filter { it.data !== null }
            .filter { it.derivedFrom === null }
            .map { it.data }
            .count().toInt()
    override val values: MutableCollection<V>
        get() = original.values.stream()
            .filter { it.data !== null }
            .filter { it.derivedFrom === null }
            .map { it.data }
            .collect(Collectors.toList())

    override fun clear() {
        original.clear()
    }

    override fun isEmpty(): Boolean = original.isEmpty()

    override fun remove(key: Class<out C>): V? {
        val data = original[key] ?: return null
        if (isNull(data)) return null

        // If it's not an independent class, then nothing will change.
        if (data.derivedFrom === null) {
            original.remove(key)
            abandonChildrenRecursively(data)
        }

        return data.data
    }

    private fun abandonChildrenRecursively(data: DataContainer) {
        // Put them up for adoption... This method name is diabolical
        data.children?.forEach {
            it.derivedFrom = null
            abandonChildrenRecursively(it)
        }
    }

    private fun updateChildrenRecursively(data: DataContainer, value: V) {
        data.children?.forEach {
            it.data = value
            updateChildrenRecursively(it, value)
        }
    }

    override fun putAll(from: Map<out Class<out C>, V>) {
        from.forEach { put(it.key, it.value) }
    }

    override fun put(key: Class<out C>, value: V): V? {
        synchronized(original) {
            val old = original[key]
            if (old !== null && old !== emptyContainer) {
                val oldData = old.data

                // Don't inherit the "derivedFrom" as we're independent now.
                old.derivedFrom?.removeChild(old)
                updateChildrenRecursively(old, value)

                return oldData
            } else {
                val new = DataContainer(value, null, null)
                original.put(key, new)
                return value
            }
        }
    }

    override fun containsValue(value: V): Boolean = values.contains(value)

    override fun toString(): String = javaClass.simpleName +
            "(" + entries.associate { it.key.name to it.value } + ")"

    fun toRealTimeString(): String = javaClass.simpleName +
            "(" + original.entries.associate { it.key.name to it.value } + ")"
}
