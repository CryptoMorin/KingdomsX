package org.kingdoms.utils.internal.numbers

import org.kingdoms.utils.internal.numbers.AnyNumber.Companion.of

enum class NumberConstraint { INTEGER_ONLY, POSITIVE, ZERO_OR_POSITIVE }
enum class NumberFailReason { NOT_A_NUMBER, OUT_OF_BOUNDS, INTEGER_ONLY, POSITIVE, ZERO_OR_POSITIVE }

class NumberProcessor(val string: String, val constraints: Set<NumberConstraint>) {
    val failedConstraints: MutableSet<NumberFailReason> = hashSetOf()
    private lateinit var _number: AnyNumber
    val number: AnyNumber
        get() {
            require(failedConstraints.isEmpty()) { "Number processor has failed" }
            return _number
        }

    fun getMostImportantFailure(): NumberFailReason {
        require(failedConstraints.isNotEmpty()) { "Number processor did not fail" }
        return failedConstraints.minByOrNull { it.ordinal }!!
    }

    val isSuccessful: Boolean get() = failedConstraints.isEmpty()

    private fun fail(reason: NumberFailReason) {
        failedConstraints.add(reason)
    }

    @Suppress("LocalVariableName")
    fun process() {
        val _number = NumberType.INT.parseString(string) ?: NumberType.DOUBLE.parseString(string)
        if (_number == null) {
            fail(NumberFailReason.NOT_A_NUMBER)
            return
        }
        this._number = _number

        if (_number.type != NumberType.INT && constraints.contains(NumberConstraint.INTEGER_ONLY)) {
            fail(NumberFailReason.INTEGER_ONLY)
        }

        if (_number <= of(_number.type.minValue) || _number >= of(_number.type.maxValue)) fail(NumberFailReason.OUT_OF_BOUNDS)
        if (constraints.contains(NumberConstraint.POSITIVE) && _number <= of(0)) fail(NumberFailReason.POSITIVE)
        if (constraints.contains(NumberConstraint.ZERO_OR_POSITIVE) && _number < of(0)) fail(NumberFailReason.ZERO_OR_POSITIVE)
    }

    companion object {
        @JvmStatic fun getNumber(string: String, constraints: Collection<NumberConstraint>): NumberProcessor {
            return NumberProcessor(string, constraints.toSet()).apply { process() }
        }
        @JvmStatic fun getNumber(string: String, vararg constraints: NumberConstraint): NumberProcessor {
            return NumberProcessor(string, constraints.toSet()).apply { process() }
        }
    }
}