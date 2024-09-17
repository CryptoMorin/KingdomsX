package org.kingdoms.utils.internal.tracker

import java.util.function.Function

class TrackedSet<K>(
    val original: MutableSet<K>,
    val onAdd: Function<K, Boolean>,
    val onRemove: Function<K, Boolean>,
) : MutableSet<K> by original {
    override fun addAll(elements: Collection<K>): Boolean {
        var changed = false
        for (element in elements) {
            if (original.add(element)) {
                onAdd.apply(element)
                changed = true
            }
        }
        return changed
    }

    override fun clear() {
        for (element in original) onRemove.apply(element)
        original.clear()
    }

    override fun iterator() = TrackedIterator(original.iterator(), onRemove)

    override fun add(element: K): Boolean {
        if (original.add(element)) {
            onAdd.apply(element)
            return true
        }
        return false
    }

    override fun remove(element: K): Boolean {
        if (original.remove(element)) {
            onRemove.apply(element)
            return true
        }
        return false
    }

    override fun removeAll(elements: Collection<K>): Boolean {
        var changed = false
        for (element in elements) {
            if (original.remove(element)) {
                onRemove.apply(element)
                changed = true
            }
        }
        return changed
    }

    override fun retainAll(elements: Collection<K>): Boolean {
        var changed = false
        for (element in original) {
            if (!elements.contains(element)) {
                onRemove.apply(element)
                changed = true
            }
        }
        return changed
    }
}

class TrackedIterator<K>(
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