package org.kingdoms.utils.internal

@Suppress("ReplaceGetOrSet") class PurifierMap<K, V>(
    private val original: MutableMap<K, V>,
    private val purifier: Purifier<K>
) : MutableMap<K, V> by original {

    companion object {
        @JvmStatic fun <K, V> MutableMap<K, V>.purify(purifier: Purifier<K>) = PurifierMap(this, purifier)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun purify(k: K) = purifier.purify(k)

    override fun remove(key: K): V? = original.remove(purify(key))
    override fun putAll(from: Map<out K, V>) = original.putAll(from.map { purify(it.key) to it.value })
    override fun put(key: K, value: V): V? = original.put(purify(key), value)
    override fun get(key: K): V? = original.get(purify(key))
    override fun containsKey(key: K): Boolean = original.containsKey(purify(key))
}