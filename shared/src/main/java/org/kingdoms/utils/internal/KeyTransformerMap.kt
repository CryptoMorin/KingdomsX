package org.kingdoms.utils.internal

import java.util.function.Function

@Suppress("ReplaceGetOrSet") class KeyTransformerMap<K, V>(
    private val original: MutableMap<K, V>,
    private val keyTransformation: Function<K, K>
) : MutableMap<K, V> by original {

    companion object {
        @JvmStatic fun <K, V> MutableMap<K, V>.purify(purifier: Function<K, K>) = KeyTransformerMap(this, purifier)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun purify(k: K) = keyTransformation.apply(k)

    override fun remove(key: K): V? = original.remove(purify(key))
    override fun putAll(from: Map<out K, V>) = original.putAll(from.map { purify(it.key) to it.value })
    override fun put(key: K, value: V): V? = original.put(purify(key), value)
    override fun get(key: K): V? = original.get(purify(key))
    override fun containsKey(key: K): Boolean = original.containsKey(purify(key))
}