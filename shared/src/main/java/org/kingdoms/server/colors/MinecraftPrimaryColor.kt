package org.kingdoms.server.colors

import org.kingdoms.utils.internal.enumeration.Enums
import java.awt.Color

interface MinecraftChatCode {
    fun getCode(): Char
}

enum class MinecraftPrimaryColor(val rgb: Int, private val code: Char) : MinecraftChatCode {
    BLACK(0x00_00_00, '0'),
    WHITE(0xFF_FF_FF, 'f'),

    DARK_BLUE(0x00_00_AA, '1'),
    DARK_GREEN(0x00_AA_00, '2'),
    DARK_AQUA(0x00_AA_AA, '3'),
    DARK_RED(0xAA_00_00, '4'),
    DARK_PURPLE(0xAA_00_AA, '5'),
    DARK_GRAY(0x55_55_55, '8'),

    GRAY(0xAA_AA_AA, '7'),
    GOLD(0xFF_AA_00, '6'),
    BLUE(0x55_55_FF, '9'),
    GREEN(0x55_FF_55, 'a'),
    AQUA(0x55_FF_FF, 'b'),
    RED(0xFF_55_55, 'c'),
    LIGHT_PURPLE(0xFF_55_FF, 'd'),
    YELLOW(0xFF_FF_55, 'e');

    override fun getCode(): Char = code

    val asJavaColor get() = Color(rgb)

    companion object {
        @JvmField val VALUES = values()
        @JvmField val RGB: MutableMap<Int, MinecraftPrimaryColor> = Enums.createMapping(VALUES) { it.rgb }
        @JvmField val COLOR_CODE: MutableMap<Char, MinecraftPrimaryColor> = Enums.createMapping(VALUES) { it.code }

        /**
         * Finds the closest MinecraftPrimaryColor to the given java.awt.Color
         * using Euclidean distance in RGB space.
         */
        @JvmStatic
        fun closestTo(color: Color): MinecraftPrimaryColor {
            var closest = BLACK
            var minDistance = Double.MAX_VALUE

            val r = color.red
            val g = color.green
            val b = color.blue

            for (mcColor in VALUES) {
                val rgb = mcColor.rgb
                val dr = r - (rgb shr 16 and 0xFF)
                val dg = g - (rgb shr 8 and 0xFF)
                val db = b - (rgb and 0xFF)

                // Make it Double right away
                val distanceSq = dr * dr + dg * dg + db * db.toDouble()

                if (distanceSq < minDistance) {
                    minDistance = distanceSq
                    closest = mcColor
                }
            }

            return closest
        }
    }
}

enum class MinecraftFormatCode(private val code: Char) : MinecraftChatCode {
    BOLD('l'),
    UNDERLINE('n'),
    STRIKETHROUGH('m'),
    OBFUSCATED('k'), // MAGIC
    ITALIC('o'),
    RESET('r'),
    ;

    override fun getCode(): Char = code
}