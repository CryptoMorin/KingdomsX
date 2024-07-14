package org.kingdoms.utils.internal.string

import org.kingdoms.utils.internal.numbers.Radix

object StringExtensions {
    fun String.toAnyInt(): Int {
        if (this.isNotEmpty()) {
            this.toIntOrNull(10)?.let { return it }
            val firstChar = this[0]
            var targetString = this

            val sign = when (firstChar) {
                '-' -> {
                    targetString = targetString.substring(1)
                    -1
                }

                '+' -> {
                    targetString = targetString.substring(1)
                    +1
                }

                else -> +1
            }

            for (radix in Radix.RADIX) {
                if (radix == Radix.DECIMAL) continue
                if (targetString.startsWith(radix.prefix)) {
                    targetString = targetString.substring(radix.prefix.length)
                    targetString.toIntOrNull(radix.radix)?.let { return it * sign }
                    break
                }
            }
        }

        error("String doesn't represent any type of number: $this")
    }
}