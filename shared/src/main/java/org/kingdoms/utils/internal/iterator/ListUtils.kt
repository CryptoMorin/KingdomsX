package org.kingdoms.utils.internal.iterator

import org.kingdoms.utils.internal.iterator.ListUtils.moveElement

object ListUtils {
    @JvmStatic fun <T> List<T>.plusAt(index: Int, vararg elements: T): List<T> {
        val result = ArrayList<T>()
        result.addAll(this.slice(0..index))
        result.addAll(elements)
        result.addAll(this.slice(index..<this.size))
        return result
    }

    @JvmStatic fun <T> List<T>.separatedBy(vararg separator: T): List<T> {
        val result = ArrayList<T>(this.size * 2)

        for (element in this) {
            if (result.isNotEmpty()) result.addAll(separator)
            result.add(element)
        }

        return result
    }

    @JvmStatic fun <T> MutableList<T>.moveOneElementCloserToStart(element: T): Boolean {
        return moveElement(element, -1)
    }

    @JvmStatic fun <T> MutableList<T>.moveOneElementCloserToEnd(element: T): Boolean {
        return moveElement(element, +1)
    }

    @JvmStatic fun <T> MutableList<T>.replace(old: T, new: T) {
        val elementIndex = this.indexOf(old)
        if (elementIndex == -1) throw IllegalArgumentException("$old is not in $this")
        this[elementIndex] = new
    }

    @JvmStatic fun <T> MutableList<T>.moveElement(element: T, relativeIndex: Int): Boolean {
        val elementIndex = this.indexOf(element)
        if (elementIndex == -1) throw IllegalArgumentException("$element is not in $this")

        val targetIndex = elementIndex + relativeIndex
        if (targetIndex < 0 || targetIndex >= this.size) return false

        val replacingElement = this[targetIndex]
        System.out.println("REPLACING $replacingElement with $element")
        this[targetIndex] = element
        this[elementIndex] = replacingElement

        return true
    }
}

private fun mapCapacity(expectedSize: Int): Int = when {
    expectedSize < 0 -> expectedSize
    expectedSize < 3 -> expectedSize + 1
    else -> ((expectedSize / 0.75F) + 1.0F).toInt()
}

public fun <K, V> Iterable<Pair<K, V>>.toMutableMap(): MutableMap<K, V> {
    return if (this is Collection) toMap(HashMap(mapCapacity(size)))
    else toMap(HashMap())
}

public fun <K, V> Iterable<Pair<K, V>>.toMutableLinkedMap(): MutableMap<K, V> {
    return if (this is Collection) toMap(LinkedHashMap(mapCapacity(size)))
    else toMap(LinkedHashMap())
}