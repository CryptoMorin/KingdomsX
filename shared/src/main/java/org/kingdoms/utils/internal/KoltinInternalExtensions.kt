@file:Suppress("NOTHING_TO_INLINE")

package org.kingdoms.utils.internal

import org.kingdoms.utils.internal.functional.Fn

inline fun <T> MutableList<T>.replaceLast(crossinline newValue: (T) -> T) {
    if (this.isEmpty()) throw IllegalStateException("Cannot replace last element in an empty list: $this")
    this[this.size - 1] = newValue(this[this.size - 1])
}

inline fun <T : Comparable<T>> T.coerceAtMostIf(condition: Boolean, crossinline max: () -> T): T {
    return if (condition) this.coerceAtMost(max()) else this
}

inline fun <T : Comparable<T>> T.coerceAtLeastIf(condition: Boolean, crossinline min: () -> T): T {
    return if (condition) this.coerceAtLeast(min()) else this
}

inline fun <T> Any?.cast(): T = Fn.cast(this)

inline fun <T> Any.safeCast(clazz: Class<T>): T? = Fn.safeCast(this, clazz)