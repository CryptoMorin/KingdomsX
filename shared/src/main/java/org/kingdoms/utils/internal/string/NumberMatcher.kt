package org.kingdoms.utils.internal.string

import org.kingdoms.utils.internal.numbers.AnyNumber
import java.util.*

interface NumberMatcher {
    fun matches(number: AnyNumber): Boolean
    val asString: String

    class Exact(private val number: AnyNumber) : NumberMatcher {
        override fun matches(number: AnyNumber): Boolean = this.number == number
        override val asString: String get() = "Exact:$number"
    }

    class LessThan(private val lessThan: AnyNumber) : NumberMatcher {
        override fun matches(number: AnyNumber): Boolean = number > this.lessThan
        override val asString: String get() = ">$lessThan"
    }

    class LessThanOrEqual(private val lessThan: AnyNumber) : NumberMatcher {
        override fun matches(number: AnyNumber): Boolean = number >= this.lessThan
        override val asString: String get() = ">=$lessThan"
    }

    class GreaterThan(private val greaterThan: AnyNumber) : NumberMatcher {
        override fun matches(number: AnyNumber): Boolean = number < this.greaterThan
        override val asString: String get() = "<$greaterThan"
        override fun toString(): String = "GreaterThan($greaterThan)"
    }

    class GreaterThanOrEqual(private val greaterThan: AnyNumber) : NumberMatcher {
        override fun matches(number: AnyNumber): Boolean = number <= this.greaterThan
        override val asString: String get() = "<=$greaterThan"
    }

    class Range(private val first: NumberMatcher, private val second: NumberMatcher) : NumberMatcher {
        override fun matches(number: AnyNumber): Boolean = first.matches(number) && second.matches(number)
        override val asString: String get() = "$first && $second"
        override fun toString(): String = "NumberMatcher::Range($first && $second)"
    }

    class Multiple(private val list: Array<AnyNumber>) : NumberMatcher {
        override fun matches(number: AnyNumber): Boolean {
            // Don't hash them as decimals can lose precision
            for (num in list) {
                if (number == num) return true
            }
            return false
        }

        override val asString: String get() = "Multiple${Arrays.toString(list)}"
    }

    private class Parser(val value: String) {
        val chars: CharArray = value.toCharArray()
        val len = chars.size
        var i = 0

        fun parse(): NumberMatcher {
            val symbol = checkSymbol()
            if (symbol !== null) {
                val num = checkNumber()
                    ?: throw IllegalArgumentException("Expected a number after comparison symbol in $value")
                return symbolToMatcher(symbol, num, Side.RIGHT_HAND)
            }

            val number = checkNumber()
            if (number !== null) {
                if (chars[i] == ',') {
                    return Multiple(value.split(',').stream()
                        .map { AnyNumber.of(it)!! }
                        .toArray { len -> arrayOfNulls(len) })
                } else {
                    // num < x < num
                    val firstSym = checkSymbol()
                    if (firstSym === null) throw IllegalArgumentException("Cant find first symbol for ternary number comparison in $value")
                    if (i >= len) return symbolToMatcher(firstSym, number, Side.LEFT_HAND)

                    val x = chars[i++]
                    if (x != 'x') throw IllegalArgumentException("Variable 'x' is not used (instead '$x') for ternary number comparison in $value")

                    val secSym = checkSymbol()
                    if (secSym === null) throw IllegalArgumentException("Cant find second symbol for ternary number comparison in $value")

                    val secNum = checkNumber()
                    if (secNum === null) throw IllegalArgumentException("Cant find end number for ternary number comparison in $value")

                    return Range(
                        first = symbolToMatcher(firstSym, number, Side.LEFT_HAND),
                        second = symbolToMatcher(secSym, secNum, Side.RIGHT_HAND)
                    )
                }
            }

            throw IllegalArgumentException("Invalid number matcher format: $value")
        }

        private enum class Side {
            RIGHT_HAND, LEFT_HAND
        }

        private fun symbolToMatcher(
            symbol: String,
            num: AnyNumber,
            numSide: Side
        ): NumberMatcher {
            return when (symbol) {
                ">" -> when (numSide) {
                    Side.RIGHT_HAND -> LessThan(num)
                    Side.LEFT_HAND -> GreaterThan(num)
                }

                ">=" -> when (numSide) {
                    Side.RIGHT_HAND -> LessThanOrEqual(num)
                    Side.LEFT_HAND -> GreaterThanOrEqual(num)
                }

                "<" -> when (numSide) {
                    Side.RIGHT_HAND -> GreaterThan(num)
                    Side.LEFT_HAND -> LessThan(num)
                }

                "<=" -> when (numSide) {
                    Side.RIGHT_HAND -> GreaterThanOrEqual(num)
                    Side.LEFT_HAND -> LessThanOrEqual(num)
                }

                else -> throw IllegalArgumentException("Unknown number comparison symbol '$symbol' in $value")
            }
        }

        private fun checkSymbol(): String? {
            if (i >= len) throw IllegalArgumentException("Expected a number comparison symbol at $i but reached the end in $value")

            val firstChar = chars[i]
            val firstSym = symbolOrNull(firstChar)
            if (firstSym === null) return null

            return if (i + 1 < len) {
                val secChar = chars[i + 1]
                val secSym = symbolOrNull(secChar)
                if (secSym === null) {
                    this.i++
                    return firstSym.toString()
                }

                this.i += 2
                return firstSym.toString() + secSym.toString()
            } else {
                this.i++
                firstSym.toString()
            }
        }

        private fun symbolOrNull(char: Char): Char? = if (char == '<' || char == '>' || char == '=') char else null

        private fun checkNumber(): AnyNumber? {
            var char: Char
            val number = StringBuilder(10)
            var i = this.i
            var sawSign = false
            var sawDecimal = false

            while (i < len) {
                char = chars[i++]
                when (char) {
                    '-' -> {
                        if (sawSign) return null
                        sawSign = true
                        number.append(char)
                    }

                    '.' -> {
                        if (sawDecimal) return null
                        sawDecimal = true
                        number.append(char)
                    }

                    in '0'..'9' -> {
                        number.append(char)
                    }

                    else -> {
                        if (number.isEmpty()) return null

                        i--
                        this.i = i
                        return AnyNumber.of(number.toString())
                    }
                }
            }

            if (number.isEmpty()) return null

            this.i = i
            return AnyNumber.of(number.toString())
        }
    }

    companion object {
        @JvmStatic fun parse(value: String?): NumberMatcher? {
            if (value.isNullOrBlank()) return null

            val sanitized = value.replace(" ", "")
            AnyNumber.of(sanitized)?.let { return Exact(it) }

            return Parser(sanitized).parse()
        }
    }
}