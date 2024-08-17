package org.kingdoms.utils.internal.arrays

import org.kingdoms.utils.internal.nonnull.NonNullList

class FunctionalList<T>(private val original: MutableList<T> = NonNullList(ArrayList())) : MutableList<T> by original {
    fun addIf(condition: Boolean, element: T): FunctionalList<T> {
        if (condition) add(element)
        return this
    }

    fun removeIf(condition: Boolean, element: T): FunctionalList<T> {
        if (condition) remove(element)
        return this
    }

    companion object {
        @JvmStatic fun <T> create(): FunctionalList<T> = FunctionalList()
    }
}
