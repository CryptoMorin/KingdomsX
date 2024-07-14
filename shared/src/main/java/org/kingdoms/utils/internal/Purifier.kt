package org.kingdoms.utils.internal

interface Purifier<T> {
    fun purify(original: T): T
}