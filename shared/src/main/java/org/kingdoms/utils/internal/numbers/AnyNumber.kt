package org.kingdoms.utils.internal.numbers

import org.kingdoms.constants.DataStringRepresentation

/**
 * An immutable, finite, non-NaN, non-Infinity number.
 * If any of the operations causes the number to become NaN or -/+Infinity,
 * a [IllegalStateException] is thrown.
 */
interface AnyNumber : Comparable<AnyNumber>, DataStringRepresentation {
    val value: Number
    val type: NumberType

    fun constructNew(value: Number): AnyNumber

    val isNegative: Boolean
    val isPositive: Boolean
    val isZero: Boolean
    val isEven: Boolean get() = rem(TWO) == ZERO
    val isOdd: Boolean get() = !isEven

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
        private val TWO = of(2)
        private val ZERO = of(0)
        @JvmStatic @get:JvmSynthetic val Number.abstractNumber: AnyNumber get() = of(this)
        @JvmStatic @get:JvmSynthetic val Float.abstractNumber: AnyNumber get() = _Float(this)
        @JvmStatic @get:JvmSynthetic val Int.abstractNumber: AnyNumber get() = _Int(this)
        @JvmStatic @get:JvmSynthetic val Double.abstractNumber: AnyNumber get() = _Double(this)

        @JvmStatic fun of(number: Number): AnyNumber = when (number) {
            is Int -> _Int(number)
            is Float -> _Float(number)
            is Double -> _Double(number)
            else -> throw UnsupportedOperationException("Unsupported number format: $number (${number.javaClass})")
        }

        @JvmStatic fun of(float: Float): AnyNumber = _Float(float)
        @JvmStatic fun of(int: Int): AnyNumber = _Int(int)
        @JvmStatic fun of(double: Double): AnyNumber = _Double(double)
        @JvmStatic fun of(string: String): AnyNumber? {
            for (type in arrayOf(NumberType.INT, NumberType.LONG, NumberType.DOUBLE)) {
                type.parseString(string)?.let { return it }
            }
            return null
        }
    }
}

interface FloatingPointNumber : AnyNumber

internal abstract class AbstractAnyNumber : AnyNumber {
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnyNumber) return false
        return this.value == other.value
    }

    final override fun hashCode(): Int = value.hashCode()
    val asString: String get() = value.toString()
    final override fun asDataString(): String = asString
    final override fun toString(): String = this.type.name + "($value)"
}

internal abstract class AbstractFloatingPointNumber : AbstractAnyNumber(), FloatingPointNumber {
    protected inline fun requireFinite(requirement: Boolean, lazyMessage: () -> String) {
        if (!requirement) throw NonFiniteNumberException(lazyMessage())
    }
}