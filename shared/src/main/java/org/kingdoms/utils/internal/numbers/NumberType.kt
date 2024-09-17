package org.kingdoms.utils.internal.numbers

enum class NumberType(
    val jvmClass: Class<out Number>,
    val minValue: Number, val maxValue: Number,
    val byteSize: Int
) {
    BYTE(Byte::class.java, Byte.MIN_VALUE, Byte.MAX_VALUE, Byte.SIZE_BYTES) {
        override fun parseStringRaw(string: String): AnyNumber = AnyNumber.of(java.lang.Byte.parseByte(string))
    },
    SHORT(Short::class.java, Short.MIN_VALUE, Short.MAX_VALUE, Short.SIZE_BYTES) {
        override fun parseStringRaw(string: String): AnyNumber = AnyNumber.of(java.lang.Short.parseShort(string))
    },
    INT(Int::class.java, Int.MIN_VALUE, Int.MAX_VALUE, Int.SIZE_BYTES) {
        override fun parseStringRaw(string: String): AnyNumber = AnyNumber.of(java.lang.Integer.parseInt(string))
    },
    LONG(Long::class.java, Long.MIN_VALUE, Long.MAX_VALUE, Long.SIZE_BYTES) {
        override fun parseStringRaw(string: String): AnyNumber = AnyNumber.of(java.lang.Long.parseLong(string))
    },
    FLOAT(Float::class.java, Float.MIN_VALUE, Float.MAX_VALUE, Float.SIZE_BYTES) {
        override fun parseStringRaw(string: String): AnyNumber = AnyNumber.of(java.lang.Float.parseFloat(string))
    },
    DOUBLE(Double::class.java, Double.MIN_VALUE, Double.MAX_VALUE, Double.SIZE_BYTES) {
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