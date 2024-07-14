package org.kingdoms.utils.internal.nonnull

import org.kingdoms.utils.internal.nonnull.NullabilityUtils.assertNonNull
import org.kingdoms.utils.internal.nonnull.NullabilityUtils.assertNonNullElements

class NonNullList<V>(val list: MutableList<V>) : MutableList<V> by list {
    override val size: Int = list.size

    constructor(size: Int) : this(ArrayList(size))

    override fun set(index: Int, element: V): V = list.set(index, assertNonNull(element))
    override fun add(element: V): Boolean = list.add(assertNonNull(element))
    override fun add(index: Int, element: V) = list.add(index, assertNonNull(element))
    override fun addAll(index: Int, elements: Collection<V>): Boolean =
        list.addAll(index, elements.assertNonNullElements())

    override fun addAll(elements: Collection<V>): Boolean = list.addAll(elements.assertNonNullElements())

    override fun remove(element: V): Boolean = list.remove(assertNonNull(element))

    override fun indexOf(element: V): Int = list.indexOf(assertNonNull(element))
    override fun lastIndexOf(element: V): Int = list.lastIndexOf(assertNonNull(element))

    override fun contains(element: V): Boolean = list.contains(assertNonNull(element))
    override fun containsAll(elements: Collection<V>): Boolean = list.containsAll(elements.assertNonNullElements())

    override fun retainAll(elements: Collection<V>): Boolean = list.retainAll(elements.assertNonNullElements().toSet())
    override fun removeAll(elements: Collection<V>): Boolean = list.removeAll(elements.assertNonNullElements().toSet())
}