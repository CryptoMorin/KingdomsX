package org.kingdoms.utils.internal.enumeration

/**
 * Memory efficient and extremely fast enum set.
 * This is only used for objects that are expected to have hashcodes
 * starting from zero. Mostly managed by a registrar that assigns these
 * hashes to them. Mostly for multi-enum hashes.
 *
 * @param <E>
 */
class OrderedMap<K, V>(size: Int) : MutableMap<K, V>, AbstractMap<K, V>() {
    private var elements: Array<Node?> = arrayOfNulls(size)
    override var size: Int = 0
    private var modCount: Int = 0

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = TODO("Not yet implemented")
    override val keys: MutableSet<K>
        get() = TODO("Not yet implemented")
    override val values: MutableCollection<V>
        get() = TODO("Not yet implemented")

    override fun clear() {
        modCount++
        size = 0
        elements.fill(null)
    }

    override fun remove(key: K): V? {
        val element = elements[key.hashCode()] ?: return null
        elements[key.hashCode()] = null
        size--
        modCount++
        return element.value
    }

    override fun get(key: K): V? = elements[key.hashCode()]?.value
    override fun containsKey(key: K): Boolean = elements[key.hashCode()] != null
    override fun containsValue(value: V): Boolean = throw UnsupportedOperationException()
    override fun hashCode(): Int = throw UnsupportedOperationException()
    override fun equals(other: Any?): Boolean = throw UnsupportedOperationException()
    override fun putAll(from: Map<out K, V>) = TODO("Not yet implemented")

    override fun put(key: K, value: V): V? {
        val hash = key.hashCode()
        val previous = get(key)

        ensureCapacity(hash)
        elements[hash] = Node(key, value)
        modCount++
        size++
        return previous
    }

    fun ensureCapacity(elementHash: Int) {
        if (elementHash > 1_000_000) throw IllegalStateException("Element hash exceeded a million")
        if (elementHash < elements.size) return
        val newElements = arrayOfNulls<Node?>(elementHash + 1)
        System.arraycopy(elements, 0, newElements, 0, elements.size)
        elements = newElements
    }

    inner class Node(val key: K, val value: V)
}