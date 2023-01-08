package org.kingdoms.peacetreaties.data

class BackedHashMap<K, V>(
    private val set: MutableSet<K>,
    private val hashMap: MutableMap<K, V>
) : MutableMap<K, V> by hashMap {
    override fun put(key: K, value: V): V? {
        set.add(key)
        return hashMap.put(key, value)
    }

    override fun remove(key: K): V? {
        set.remove(key)
        return hashMap.remove(key)
    }

    override fun clear() {
        set.clear()
        hashMap.clear()
    }
}