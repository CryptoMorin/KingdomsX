package org.kingdoms.utils.internal.tracker

import java.util.function.BiConsumer
import java.util.function.Function

class TrackedMap<K, V>(
    val original: MutableMap<K, V>,
    val onAdd: BiConsumer<K, V>,
    val onRemove: BiConsumer<K, V>,
) : MutableMap<K, V> by original {
    class BackedMap<K, V : V2, V2>(private val original: MutableMap<K, V2>, private val adder: Boolean) :
        BiConsumer<K, V> {
        override fun accept(t: K, u: V) {
            if (adder) original[t] = u
            else original.remove(t)
        }
    }

    companion object {
        @JvmStatic
        fun <K, V2> backedBy(current: MutableMap<K, out V2>, original: MutableMap<K, V2>): MutableMap<K, out V2> {
            //  public static <K, V2> Map<K, ? extends V2> ofSelf(Map<K, ? extends V2> current, Map<K, V2> original) {
            //         return new TrackedMap<>(current, new BackedMap<>(original, true), new BackedMap<>(original, false));
            //  }
            return TrackedMap(current, BackedMap(original, true), BackedMap(original, false))
        }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = TrackedSet(original.entries,
            onAdd = Function<MutableMap.MutableEntry<K, V>, Boolean> { entry ->
                this.onAdd.accept(entry.key, entry.value)
                return@Function true
            },
            onRemove = Function<MutableMap.MutableEntry<K, V>, Boolean> { (key, value) ->
                this.onRemove.accept(key, value)
                return@Function true
            }
        )

    override val keys: MutableSet<K>
        get() = TrackedSet(original.keys,
            { _ -> throw UnsupportedOperationException() },
            { _ -> throw UnsupportedOperationException() }
        )


    override val values: MutableCollection<V>
        get() = TrackedCollection(original.values,
            onAdd = { _ -> throw UnsupportedOperationException() },
            onRemove = { _ -> throw UnsupportedOperationException() }
        )

    override fun clear() {
        original.entries.forEach { (key, value) -> onRemove.accept(key, value) }
        original.clear()
    }

    override fun remove(key: K): V? {
        val removed = original.remove(key)
        if (removed != null) onRemove.accept(key, removed)
        return removed
    }

    override fun putAll(from: Map<out K, V>) {
        for (entry in from) {
            original[entry.key] = entry.value
            onAdd.accept(entry.key, entry.value)
        }
    }

    override fun put(key: K, value: V): V? {
        onAdd.accept(key, value)
        return original.put(key, value)
    }

    override fun toString(): String {
        return "${javaClass.simpleName}($original)"
    }
}

class TrackedMapIterator<K>(
    val original: MutableIterator<K>,
    val onRemove: Function<K, Boolean>,
) :
    MutableIterator<K> by original {
    var next: K? = null

    override fun remove() {
        if (next == null) original.remove()
        if (!onRemove.apply(next!!)) return
        original.remove()
    }

    override fun next(): K {
        val n = original.next()
        this.next = n
        return n
    }
}