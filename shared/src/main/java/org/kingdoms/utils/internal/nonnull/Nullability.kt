package org.kingdoms.utils.internal.nonnull

object Nullability {
    @JvmStatic
    fun <T> Collection<T>.assertNonNullElements(): Collection<T> {
        if (this.any { x -> x == null }) throw IllegalArgumentException("${this.javaClass.simpleName} contains null")
        return this
    }

    @JvmStatic
    fun <T> Collection<T>.assertNonNull(obj: T?): T {
        return obj ?: throw IllegalArgumentException("${this.javaClass.simpleName} cannot contain null values")
    }

    @JvmStatic
    fun <T> T?.assertNotNull(msg: String): T {
        if (this == null) throw NullPointerException(msg)
        return this
    }
}