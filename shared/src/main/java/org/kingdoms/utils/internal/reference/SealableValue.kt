package org.kingdoms.utils.internal.reference

class SealableValue<T>(value: T? = null) {
    private var valueRef: T? = value
    var isSealed: Boolean = false

    fun setAndSeal(value: T) {
        if (isSealed) return
        this.valueRef = value
        isSealed = true
    }

    fun set(value: T) {
        if (isSealed) return
        this.valueRef = value
    }

    fun get(): T? = valueRef
}