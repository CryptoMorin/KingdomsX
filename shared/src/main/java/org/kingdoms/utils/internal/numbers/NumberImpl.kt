@file:Suppress("ClassName")

package org.kingdoms.utils.internal.numbers

internal class _Int(private var jvmValue: Int) : AnyNumber {
    override val type = NumberType.INT
    override val value = this.jvmValue

    private inline val AnyNumber.toThis: Int get() = this.value.toInt()
    override fun constructNew(value: Number): AnyNumber = _Int(value.toInt())

    override fun unaryMinus() = constructNew(-jvmValue)
    override fun unaryPlus() = constructNew(+jvmValue)
    override fun inc() = constructNew(jvmValue + 1)
    override fun dec() = constructNew(jvmValue - 1)

    override fun plus(other: AnyNumber) = constructNew(jvmValue + other.toThis)
    override fun minus(other: AnyNumber) = constructNew(jvmValue - other.toThis)
    override fun times(other: AnyNumber) = constructNew(jvmValue * other.toThis)
    override fun div(other: AnyNumber) = constructNew(jvmValue / other.toThis)
    override fun rem(other: AnyNumber) = constructNew(jvmValue % other.toThis)

    override fun compareTo(other: AnyNumber) = jvmValue.compareTo(other.toThis)
}

internal class _Float(private var jvmValue: Float) : AnyNumber {
    override val type = NumberType.FLOAT
    override val value = this.jvmValue

    private inline val AnyNumber.toThis: Float get() = this.value.toFloat()
    override fun constructNew(value: Number): AnyNumber = _Float(value.toFloat())

    override fun unaryMinus() = constructNew(-jvmValue)
    override fun unaryPlus() = constructNew(+jvmValue)
    override fun inc() = constructNew(jvmValue + 1)
    override fun dec() = constructNew(jvmValue - 1)

    override fun plus(other: AnyNumber) = constructNew(jvmValue + other.toThis)
    override fun minus(other: AnyNumber) = constructNew(jvmValue - other.toThis)
    override fun times(other: AnyNumber) = constructNew(jvmValue * other.toThis)
    override fun div(other: AnyNumber) = constructNew(jvmValue / other.toThis)
    override fun rem(other: AnyNumber) = constructNew(jvmValue % other.toThis)

    override fun compareTo(other: AnyNumber) = jvmValue.compareTo(other.toThis)
}