package org.kingdoms.utils.internal.nonnull

import org.kingdoms.utils.internal.nonnull.Nullability.assertNonNull
import org.kingdoms.utils.internal.nonnull.Nullability.assertNonNullElements

class NonNullSet<V>(val set: MutableSet<V> = hashSetOf()) : MutableSet<V> by set {
    override val size: Int = set.size

    override fun addAll(elements: Collection<V>): Boolean = set.addAll(elements.assertNonNullElements())
    override fun add(element: V): Boolean = set.add(this.assertNonNull(element))
    override fun remove(element: V): Boolean = set.remove(this.assertNonNull(element))

    override fun contains(element: V): Boolean = set.contains(this.assertNonNull(element))
    override fun containsAll(elements: Collection<V>): Boolean = set.containsAll(elements.assertNonNullElements())

    override fun retainAll(elements: Collection<V>): Boolean = set.retainAll(elements.assertNonNullElements().toSet())
    override fun removeAll(elements: Collection<V>): Boolean = set.removeAll(elements.assertNonNullElements().toSet())

    override fun hashCode(): Int =
        throw UnsupportedOperationException("Hashcodes are not supported for this implementation")

    override fun toString(): String = "${javaClass.simpleName}($set)"
}