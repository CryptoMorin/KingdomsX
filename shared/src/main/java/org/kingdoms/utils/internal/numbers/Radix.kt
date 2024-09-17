package org.kingdoms.utils.internal.numbers

enum class Radix(val radix: Int, val prefix: String) {
    BINARY(2, "0b"), OCTAL(8, "0o"), DECIMAL(10, ""), HEXADECIMAL(16, "0x");

    companion object {
        @Suppress("EnumValuesSoftDeprecate")
        @JvmField val RADIX: Array<Radix> = values()
    }
}