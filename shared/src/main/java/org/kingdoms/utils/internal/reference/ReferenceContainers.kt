package org.kingdoms.utils.internal.reference

private class MapEntry<K, V>(
    override val key: K,
    override val value: V
) : Map.Entry<K, V>

class ReferencedMap<K, V, R : Reference<V>>(val map: MutableMap<K, R>) : Map<K, V> {
    override val entries: Set<Map.Entry<K, V>>
        get() = map.entries.filter { it.value.exists() }.mapTo(hashSetOf()) { MapEntry(it.key, it.value.get()) }
    override val keys: Set<K>
        get() = map.entries.filter { it.value.exists() }.mapTo(hashSetOf()) { it.key }
    override val values: Collection<V>
        get() = map.entries.filter { it.value.exists() }.mapTo(hashSetOf()) { it.value.get() }
    override val size: Int
        get() = map.entries.filter { it.value.exists() }.size

    @Suppress("ReplaceSizeZeroCheckWithIsEmpty")
    override fun isEmpty(): Boolean = size == 0

    override fun get(key: K): V? {
        val value = (map[key] ?: return null)
        return if (value.exists()) value.get() else null
    }

    override fun containsValue(value: V): Boolean = values.contains(value)
    override fun containsKey(key: K): Boolean = keys.contains(key)
}