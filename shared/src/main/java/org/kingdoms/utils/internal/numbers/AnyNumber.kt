package org.kingdoms.utils.internal.numbers

enum class NumberType(
    val jvmClass: Class<out Number>,
    val minValue: Number, val maxValue: Number,
    val byteSize: Int
) {
    BYTE(Byte::class.java, Byte.MIN_VALUE, Byte.MAX_VALUE, Byte.SIZE_BYTES),
    SHORT(Short::class.java, Short.MIN_VALUE, Short.MAX_VALUE, Short.SIZE_BYTES),
    INT(Int::class.java, Int.MIN_VALUE, Int.MAX_VALUE, Int.SIZE_BYTES),
    LONG(Long::class.java, Long.MIN_VALUE, Long.MAX_VALUE, Long.SIZE_BYTES),
    FLOAT(Float::class.java, Float.MIN_VALUE, Float.MAX_VALUE, Float.SIZE_BYTES),
    DOUBLE(Double::class.java, Double.MIN_VALUE, Double.MAX_VALUE, Double.SIZE_BYTES)
}

interface AnyNumber : Comparable<AnyNumber> {
    val value: Number
    val type: NumberType

    fun constructNew(value: Number): AnyNumber

    operator fun unaryMinus(): AnyNumber
    operator fun unaryPlus(): AnyNumber
    operator fun inc(): AnyNumber
    operator fun dec(): AnyNumber

    operator fun plus(other: AnyNumber): AnyNumber
    operator fun minus(other: AnyNumber): AnyNumber
    operator fun times(other: AnyNumber): AnyNumber
    operator fun div(other: AnyNumber): AnyNumber
    operator fun rem(other: AnyNumber): AnyNumber

    companion object {
        @JvmStatic @get:JvmName("abstractNumber") val Float.abstractNumber: AnyNumber get() = _Float(this)
        @JvmStatic @get:JvmName("abstractNumber") val Int.abstractNumber: AnyNumber get() = _Int(this)
    }
}
