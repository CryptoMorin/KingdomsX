package org.kingdoms.utils.internal.arrays

object KotlinArrayExtensions {
    inline fun <T> Array<T>.require(crossinline requirement: (T) -> Boolean, failMessage: (T) -> Any): Array<T> {
        for (element in this) {
            check(requirement(element)) { failMessage(element) }
        }
        return this
    }

    inline fun <reified T> Array<T>.insertAt(index: Int, elements: Sequence<T>): Array<T> {
        val result = ArrayList<T>()
        result.addAll(this.slice(0..index))
        result.addAll(elements)
        result.addAll(this.slice(index..this.size))
        return result.toTypedArray()
    }
}