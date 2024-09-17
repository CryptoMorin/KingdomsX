@file:Suppress("ClassName")

package org.kingdoms.utils.internal.numbers

internal class _Int(override val value: Int) : AbstractAnyNumber() {
    override val type = NumberType.INT

    override val isNegative: Boolean get() = value < 0
    override val isPositive: Boolean get() = value > 0
    override val isZero: Boolean get() = value == 0

    private inline val AnyNumber.convert: Int get() = this.value.toInt()
    override fun constructNew(value: Number): AnyNumber = _Int(value.toInt())

    override fun unaryMinus() = constructNew(-value)
    override fun unaryPlus() = constructNew(+value)
    override fun inc() = constructNew(value + 1)
    override fun dec() = constructNew(value - 1)

    override fun plus(other: AnyNumber) = constructNew(value + other.convert)
    override fun minus(other: AnyNumber) = constructNew(value - other.convert)
    override fun times(other: AnyNumber) = constructNew(value * other.convert)
    override fun div(other: AnyNumber) = constructNew(value / other.convert)
    override fun rem(other: AnyNumber) = constructNew(value % other.convert)

    override fun compareTo(other: AnyNumber) = value.compareTo(other.convert)
}

internal class _Float(override val value: Float) : AbstractFloatingPointNumber() {
    override val type = NumberType.FLOAT

    override val isNegative: Boolean get() = value < 0f
    override val isPositive: Boolean get() = value > 0f
    override val isZero: Boolean get() = value == 0f

    init {
        requireFinite(!value.isNaN()) { "Value is not finite, but NaN: $value" }
        requireFinite(!value.isInfinite()) { "Value is not finite, but Infinity: $value" }
    }

    private inline val AnyNumber.convert: Float get() = this.value.toFloat()
    override fun constructNew(value: Number): AnyNumber = _Float(value.toFloat())

    override fun unaryMinus() = constructNew(-value)
    override fun unaryPlus() = constructNew(+value)
    override fun inc() = constructNew(value + 1f)
    override fun dec() = constructNew(value - 1f)

    override fun plus(other: AnyNumber) = constructNew(value + other.convert)
    override fun minus(other: AnyNumber) = constructNew(value - other.convert)
    override fun times(other: AnyNumber) = constructNew(value * other.convert)
    override fun div(other: AnyNumber) = constructNew(value / other.convert)
    override fun rem(other: AnyNumber) = constructNew(value % other.convert)

    override fun compareTo(other: AnyNumber) = value.compareTo(other.convert)
}

internal class _Double(override val value: Double) : AbstractFloatingPointNumber() {
    override val type = NumberType.DOUBLE

    override val isNegative: Boolean get() = value < 0.0
    override val isPositive: Boolean get() = value > 0.0
    override val isZero: Boolean get() = value == 0.0

    init {
        requireFinite(!value.isNaN()) { "Value is not finite, but NaN: $value" }
        requireFinite(!value.isInfinite()) { "Value is not finite, but Infinity: $value" }
    }

    private inline val AnyNumber.convert: Double get() = this.value.toDouble()
    override fun constructNew(value: Number): AnyNumber = _Double(value.toDouble())

    override fun unaryMinus() = constructNew(-value)
    override fun unaryPlus() = constructNew(+value)
    override fun inc() = constructNew(value + 1.0)
    override fun dec() = constructNew(value - 1.0)

    override fun plus(other: AnyNumber) = constructNew(value + other.convert)
    override fun minus(other: AnyNumber) = constructNew(value - other.convert)
    override fun times(other: AnyNumber) = constructNew(value * other.convert)
    override fun div(other: AnyNumber) = constructNew(value / other.convert)
    override fun rem(other: AnyNumber) = constructNew(value % other.convert)

    override fun compareTo(other: AnyNumber) = value.compareTo(other.convert)
}