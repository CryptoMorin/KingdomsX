package org.kingdoms.utils.internal.numbers

import org.kingdoms.utils.internal.numbers.AnyNumber.Companion.abstractNumber
import org.kingdoms.utils.internal.numbers.AnyNumber.Companion.of

enum class NumberDecorator { SUFFIX, COMMA }
enum class NumberConstraint { INTEGER_ONLY, POSITIVE, ZERO_OR_POSITIVE }
enum class NumberFailReason { NOT_A_NUMBER, OUT_OF_BOUNDS, INTEGER_ONLY, POSITIVE, ZERO_OR_POSITIVE }

/**
 * A class used for processing strings into numbers.
 */
class NumberProcessor(val string: String) {
    val failedConstraints: MutableSet<NumberFailReason> = hashSetOf()
    private var constraints: Set<NumberConstraint> = hashSetOf()
    private var decorators: Set<NumberDecorator> = hashSetOf()
    private lateinit var _number: AnyNumber
    private var processed = false
    val number: AnyNumber
        get() {
            require(failedConstraints.isEmpty()) { "Number processor has failed" }
            return _number
        }

    fun withConstraints(vararg constraints: NumberConstraint): NumberProcessor {
        this.constraints = constraints.toSet()
        return this
    }

    fun withAllDecorators(): NumberProcessor {
        this.decorators = ALL_DECORATORS
        return this
    }

    fun withDecorators(vararg decorators: NumberDecorator): NumberProcessor {
        this.decorators = decorators.toSet()
        return this
    }

    fun getMostImportantFailure(): NumberFailReason {
        require(failedConstraints.isNotEmpty()) { "Number processor did not fail" }
        return failedConstraints.minByOrNull { it.ordinal }!!
    }

    val isSuccessful: Boolean
        get() {
            require(processed) { "$string is not processed yet" }
            return failedConstraints.isEmpty()
        }

    private fun fail(reason: NumberFailReason) {
        failedConstraints.add(reason)
    }

    @Suppress("LocalVariableName")
    fun process() {
        require(!processed) { "$string is already processed" }
        processed = true

        var mod: Int? = null
        val _number = string.let {
            var str = it
            if (decorators.contains(NumberDecorator.SUFFIX)) {
                // 15k -> 15000
                str = if (str.endsWith('K', ignoreCase = true)) {
                    mod = 1_000
                    str.substring(0, str.length - 1)
                } else {
                    str
                }
            }
            if (decorators.contains(NumberDecorator.COMMA)) {
                // 15,000,000 -> 15000000
                str = str.replace(",", "")
            }
            str
        }.let {
            NumberType.INT.parseString(it) ?: NumberType.DOUBLE.parseString(it)
        }
        if (_number == null) {
            fail(NumberFailReason.NOT_A_NUMBER)
            return
        }

        if (mod === null) this._number = _number
        else this._number = _number.times(mod!!.abstractNumber)

        if (_number.type != NumberType.INT && constraints.contains(NumberConstraint.INTEGER_ONLY)) {
            fail(NumberFailReason.INTEGER_ONLY)
        }

        if (_number <= of(_number.type.minValue) || _number >= of(_number.type.maxValue)) fail(NumberFailReason.OUT_OF_BOUNDS)
        if (constraints.contains(NumberConstraint.POSITIVE) && _number <= of(0)) fail(NumberFailReason.POSITIVE)
        if (constraints.contains(NumberConstraint.ZERO_OR_POSITIVE) && _number < of(0)) fail(NumberFailReason.ZERO_OR_POSITIVE)
    }

    companion object {
        @JvmStatic private val ALL_DECORATORS = NumberDecorator.values().toSet()
    }
}