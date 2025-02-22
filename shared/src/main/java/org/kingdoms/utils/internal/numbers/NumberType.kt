package org.kingdoms.utils.internal.numbers

/**
 * Note we can't use the `MIN_VALUE` codes for numbers as they don't
 * actually represent the minimum value that the number type can hold...
 */
enum class NumberType(
    val jvmClass: Class<out Number>,
    val minValue: Number, val maxValue: Number,
    val byteSize: Int
) {
    BYTE(Byte::class.java, -Byte.MAX_VALUE, Byte.MAX_VALUE, Byte.SIZE_BYTES) {
        override fun parseStringRaw(string: String): AnyNumber = AnyNumber.of(java.lang.Byte.parseByte(string))
    },
    SHORT(Short::class.java, -Short.MAX_VALUE, Short.MAX_VALUE, Short.SIZE_BYTES) {
        override fun parseStringRaw(string: String): AnyNumber = AnyNumber.of(java.lang.Short.parseShort(string))
    },
    INT(Int::class.java, -Int.MAX_VALUE, Int.MAX_VALUE, Int.SIZE_BYTES) {
        override fun parseStringRaw(string: String): AnyNumber = AnyNumber.of(java.lang.Integer.parseInt(string))
    },
    LONG(Long::class.java, -Long.MAX_VALUE, Long.MAX_VALUE, Long.SIZE_BYTES) {
        override fun parseStringRaw(string: String): AnyNumber = AnyNumber.of(java.lang.Long.parseLong(string))
    },
    FLOAT(Float::class.java, -Float.MAX_VALUE, Float.MAX_VALUE, Float.SIZE_BYTES) {
        override fun parseStringRaw(string: String): AnyNumber = AnyNumber.of(java.lang.Float.parseFloat(string))
    },
    DOUBLE(Double::class.java, -Double.MAX_VALUE, Double.MAX_VALUE, Double.SIZE_BYTES) {
        override fun parseStringRaw(string: String): AnyNumber = AnyNumber.of(java.lang.Double.parseDouble(string))
    };

    // val minValue: AnyNumber = AnyNumber.of(minValue)
    // val maxValue: AnyNumber = AnyNumber.of(maxValue)

    protected abstract fun parseStringRaw(string: String): AnyNumber

    fun parseString(string: String): AnyNumber? = try {
        parseStringRaw(string)
    } catch (ignored: NumberFormatException) {
        null
    }

    val isFloatingPoint: Boolean get() = this == FLOAT || this == DOUBLE
}