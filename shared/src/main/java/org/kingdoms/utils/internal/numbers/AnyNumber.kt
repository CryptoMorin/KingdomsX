package org.kingdoms.utils.internal.numbers

import org.kingdoms.constants.DataStringRepresentation

/**
 * An immutable, finite, non-NaN, non-Infinity number.
 * If any of the operations causes the number to become NaN or -/+Infinity,
 * a [IllegalStateException] is thrown.
 */
@Suppress("INAPPLICABLE_JVM_NAME")
interface AnyNumber : Comparable<AnyNumber>, DataStringRepresentation {
    val value: Number
    val type: NumberType

    fun constructNew(value: Number): AnyNumber

    @get:JvmName("isNegative") val isNegative: Boolean
    @get:JvmName("isPositive") val isPositive: Boolean
    @get:JvmName("isZero") val isZero: Boolean
    @get:JvmName("isEven") val isEven: Boolean get() = rem(TWO) == ZERO
    @get:JvmName("isOdd") val isOdd: Boolean get() = !isEven
    @get:JvmName("hasDecimals") val hasDecimals: Boolean get() = this is FloatingPointNumber

    fun abs(): AnyNumber = if (this < ZERO) unaryMinus() else this
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
            is Double -> _Double(number)
            is Float -> _Float(number)
            is Long -> _Long(number)
            else -> throw UnsupportedOperationException("Unsupported number format: $number (${number.javaClass})")
        }

        @JvmStatic fun of(float: Long): AnyNumber = _Long(float)
        @JvmStatic fun of(float: Float): AnyNumber = _Float(float)
        @JvmStatic fun of(int: Int): AnyNumber = _Int(int)
        @JvmStatic fun of(double: Double): AnyNumber = _Double(double)
        @JvmStatic fun of(string: String): AnyNumber? {
            if (string.contains('.')) {
                return NumberType.DOUBLE.parseString(string)
            }

            for (type in arrayOf(NumberType.INT, NumberType.LONG, NumberType.DOUBLE)) {
                type.parseString(string)?.let { return it }
            }
            return null
        }
    }
}

interface FloatingPointNumber : AnyNumber {
    val isNaN: Boolean
    val isPositiveInfinity: Boolean
    val isNegativeInfinity: Boolean
    val isInfinite: Boolean
}

internal abstract class AbstractAnyNumber : AnyNumber {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnyNumber) return false
        if (this.type === other.type) return this.value == other.value

        // Use double value, because if object wrappers for nubmers
        // do equality test, it'll only succeed if the numbers are the same type.
        // Note that this will stop working if we ever use the "BigNumber" classes.
        return this.value.toLong() == other.value.toLong()
    }

    final override fun hashCode(): Int = value.hashCode()
    val asString: String get() = value.toString()
    final override fun asDataString(): String = asString
    final override fun toString(): String = this.type.name + "($value)"
}

@Suppress("EqualsOrHashCode")
internal abstract class AbstractFloatingPointNumber : AbstractAnyNumber(), FloatingPointNumber {
    protected inline fun requireFinite(requirement: Boolean, lazyMessage: () -> String) {
        if (!requirement) throw NonFiniteNumberException(lazyMessage())
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnyNumber) return false
        if (this.type === other.type) return this.value == other.value

        // 0.1f != 0.1D
        // Devs seem to suggest that using BigDecimal is recommended.
        // But do we really need it for this?
        return this.value.toDouble() == other.value.toDouble() ||
                this.value.toString() == other.value.toString()
    }
}