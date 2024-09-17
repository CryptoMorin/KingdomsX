package org.kingdoms.utils.internal.reference

import org.kingdoms.utils.internal.reference.Reference.Companion.throwEmpty
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

interface Reference<T> {
    /**
     * @throws NoSuchElementException if [exists] is false.
     */
    fun get(): T
    fun orElse(default: T) = if (exists()) get() else default

    fun set(value: T)
    fun exists(): Boolean
    fun remove()

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        @JvmStatic
        inline fun throwEmpty(): Nothing = throw NoSuchElementException("No value is present")
    }
}

class BoolReference(private var value: AtomicBoolean?) : Reference<Boolean> {
    override fun get() = (value ?: throwEmpty()).get()
    override fun exists(): Boolean = value != null
    override fun remove() {
        value = null
    }

    override fun set(value: Boolean) {
        if (this.value == null) this.value = AtomicBoolean(value)
        else this.value!!.set(value)
    }
}

class ObjectReference<O>(private var value: AtomicReference<O>) : Reference<O> {
    override fun get() = (value.get() ?: throwEmpty())
    override fun exists(): Boolean = value.get() != null
    override fun remove() {
        value.set(null)
    }

    override fun set(value: O) {
        this.value.set(value)
    }
}