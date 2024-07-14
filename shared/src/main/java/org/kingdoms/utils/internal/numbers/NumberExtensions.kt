package org.kingdoms.utils.internal.numbers

@Suppress("NOTHING_TO_INLINE")
object NumberExtensions {
    inline fun Double.squared() = this * this
    inline fun Float.squared() = this * this
    inline fun Int.squared() = this * this
}