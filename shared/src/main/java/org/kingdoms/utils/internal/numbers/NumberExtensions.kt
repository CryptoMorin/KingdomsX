package org.kingdoms.utils.internal.numbers

@Suppress("NOTHING_TO_INLINE")
object NumberExtensions {
    inline fun Double.squared() = this * this
    inline fun Float.squared() = this * this
    inline fun Int.squared() = this * this

    inline val Number.isEven: Boolean get() = (this.toInt() % 2) == 0

    @JvmStatic
    fun Number.requireNonNegative() {
        if (AnyNumber.of(this).isNegative) {
            throw IllegalStateException("Required a non-negative number, but got: $this")
        }
    }
}