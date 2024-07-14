package org.kingdoms.utils.internal.arrays

import org.kingdoms.utils.internal.nonnull.NullabilityUtils.assertNonNull
import org.kingdoms.utils.internal.nonnull.NullabilityUtils.assertNonNullElements

@Suppress("UNCHECKED_CAST")
class FixedArrayList<E>(size: Int) : AbstractList<E?>(), MutableList<E?>, RandomAccess, Cloneable,
    java.io.Serializable {
    private val array: Array<Any?> = arrayOfNulls(size)
    override var size: Int = 0

    override fun clear() {
        array.fill(null)
        size = 0
    }

    override fun addAll(elements: Collection<E?>): Boolean {
        elements.forEach { x -> add(x) }
        return true
    }

    override fun addAll(index: Int, elements: Collection<E?>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun add(index: Int, element: E?) {
        throw UnsupportedOperationException()
    }

    override fun add(element: E?): Boolean {
        checkFull()
        array[size++] = element
        return true
    }

    private fun checkFull() {
        if (isFull()) throw IllegalStateException("List already full")
    }

    private fun checkIndex(index: Int) {
        if (index >= size) throw IndexOutOfBoundsException("$index >= $size")
        if (index < 0) throw IllegalArgumentException("Index is negative: $index")
    }

    fun isFull() = size == array.size

    override fun get(index: Int): E {
        checkIndex(index)
        return array[index] as E
    }

    override fun iterator(): MutableIterator<E> = Iterator()
    inner class Iterator : MutableIterator<E> {
        private var index = 0

        override fun hasNext(): Boolean = index < size
        override fun next(): E {
            if (!hasNext()) throw NoSuchElementException("Index: $index")
            return array[index++] as E
        }

        override fun remove() {

        }

    }

    override fun listIterator(): MutableListIterator<E?> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): MutableListIterator<E?> {
        TODO("Not yet implemented")
    }

    override fun removeAt(index: Int): E? {
        checkIndex(index)
        val prev = array[index]
        array[index] = null
        size--
        return prev as E?
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E?> {
        throw UnsupportedOperationException()
    }

    override fun set(index: Int, element: E?): E? {
        assertNonNull(element)
        checkIndex(index)
        val prev = array[index]
        array[index] = element
        return prev as E
    }

    override fun retainAll(elements: Collection<E?>): Boolean {
        elements.assertNonNullElements()

        var modified = false
        for ((index, element) in array.withIndex()) {
            if (!elements.contains(element)) {
                array[index] = null
                modified = true
            }
        }
        return modified
    }

    override fun removeAll(elements: Collection<E?>): Boolean {
        elements.assertNonNullElements()

        var modified = false
        for (element in elements) {
            if (remove(element)) modified = true
        }
        return modified
    }

    override fun remove(element: E?): Boolean {
        assertNonNull(element)
        for ((index, currEle) in array.withIndex()) {
            if (currEle == element) {
                array[index] = null
                return true
            }
        }
        return false
    }

    override fun contains(element: E?): Boolean = array.contains(assertNonNull(element))
}