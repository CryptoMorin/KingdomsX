package org.kingdoms.utils.internal.enumeration

/**
 * Memory efficient and extremely fast enum set.
 * This is only used for objects that are expected to have hashcodes
 * starting from zero. Mostly managed by a registrar that assigns these
 * hashes to them. Mostly for multi-enum hashes.
 *
 * @param <E>
 */
class OrderedSet<E>(size: Int) : MutableSet<E>, AbstractSet<E>() {
    private var elements: Array<Any?> = arrayOfNulls(size)
    override var size: Int = 0
    private var modCount: Int = 0

    constructor(collection: Collection<E>) : this(collection.size) {
        addAll(collection)
    }

    override fun add(element: E): Boolean {
        val hash = element.hashCode()
        if (internalContains(hash)) return true

        modCount++
        ensureCapacity(hash)
        elements[hash] = element
        size++
        return false
    }

    fun ensureCapacity(elementHash: Int) {
        if (elementHash < elements.size) return
        val newElements = arrayOfNulls<Any?>(elementHash + 1)
        System.arraycopy(elements, 0, newElements, 0, elements.size)
        elements = newElements
    }

    override fun addAll(elements: Collection<E>): Boolean {
        ensureCapacity(elements.size)
        modCount++
        for (element in elements) add(element)
        return true
    }

    override fun clear() {
        modCount++
        elements.fill(null)
        size = 0
    }

    override fun iterator(): MutableIterator<E> = Iterator()

    override fun remove(element: E): Boolean {
        val hash = element.hashCode()
        val contained = internalContains(hash)
        if (contained) {
            modCount++
            elements[hash] = null
            size--
        }
        return contained
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        modCount++
        for (element in elements) remove(element)
        return true
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        val iter = iterator()
        modCount++
        while (iter.hasNext()) {
            val next = iter.next()
            if (!elements.contains(next)) iter.remove()
        }

        return true
    }

    override fun contains(element: E): Boolean = internalContains(element.hashCode())

    @Suppress("NOTHING_TO_INLINE")
    private inline fun internalContains(hash: Int): Boolean = hash < elements.size && elements[hash] != null

    override fun containsAll(elements: Collection<E>): Boolean {
        for (element in elements) {
            if (!contains(element)) return false
        }
        return true
    }

    override fun isEmpty(): Boolean = size == 0

    inner class Iterator<E> : MutableIterator<E> {
        var cursor: Int = 0
        private var iterModCount = modCount

        override fun hasNext(): Boolean {
            checkModCount()
            while (true) {
                if (cursor == elements.size) return false
                val element = elements[cursor]
                if (element != null) return true
                cursor++
            }
        }


        override fun next(): E {
            if (!hasNext()) throw NoSuchElementException("Size=$size")

            @Suppress("UNCHECKED_CAST")
            return elements[cursor++] as E
        }

        private fun checkModCount() {
            if (iterModCount != modCount) throw ConcurrentModificationException()
        }

        override fun remove() {
            checkModCount()
            if (elements[cursor] == null) throw IllegalStateException("Element already removed, next() not called")
            elements[cursor] = null
            iterModCount = ++modCount
            size--
        }
    }
}