package org.kingdoms.server.colors

import org.kingdoms.utils.internal.enumeration.Enums

interface MinecraftChatCode {
    fun getCode(): Char
}

enum class MinecraftPrimaryColor(val RGB: Int, private val code: Char) : MinecraftChatCode {
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

    companion object {
        @JvmField val VALUES = MinecraftPrimaryColor.values()
        @JvmField val RGB: MutableMap<Int, MinecraftPrimaryColor> = Enums.createMapping(VALUES) { it.RGB }
        @JvmField val COLOR_CODE: MutableMap<Char, MinecraftPrimaryColor> = Enums.createMapping(VALUES) { it.code }
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