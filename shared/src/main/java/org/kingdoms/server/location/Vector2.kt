package org.kingdoms.server.location

import org.kingdoms.constants.DataStringRepresentation
import org.kingdoms.utils.internal.string.CommaDataSplitStrategy

interface Point2D {
    val x: Double
    val z: Double
}

class Vector2(override val x: Double, override val z: Double) : Point2D {
    override fun hashCode(): Int {
        /**
         * From Minecraft's ChunkCoordIntPair
         */
        // int var2 = 1664525 * x + 1013904223;
        // int var3 = 1664525 * (z ^ -559038737) + 1013904223;
        // return var2 ^ var3;

        var bits = java.lang.Double.doubleToLongBits(x)
        bits = bits xor java.lang.Double.doubleToLongBits(z) * 31
        return ((bits.toInt()) xor ((bits shr 32).toInt()))
    }

    override fun equals(obj: Any?): Boolean {
        val other = obj as? Point2D ?: return false
        return this.x == other.x && this.z == other.z
    }

    override fun toString(): String {
        return "Vector2($x, $z)"
    }
}

interface BlockPoint2D {
    val x: Int
    val z: Int
}

class BlockVector2(override val x: Int, override val z: Int) : BlockPoint2D, DataStringRepresentation {
    override fun asDataString(): String = CommaDataSplitStrategy.toString(x, z)

    companion object {
        const val CHUNK_SHIFTS: Int = 4
        const val CHUNK_SHIFTS_Y: Int = 8

        @JvmStatic
        fun of(other: BlockPoint2D): BlockVector2 = of(other.x, other.z)

        @JvmStatic
        fun of(x: Int, z: Int): BlockVector2 = BlockVector2(x, z)

        @JvmStatic
        fun fromString(str: String): BlockVector2 = CommaDataSplitStrategy(
            str,
            2
        ).run {
            BlockVector2(nextInt(), nextInt())
        }
    }

    override fun hashCode(): Int {
        return (z shl 16) xor x
    }

    override fun equals(obj: Any?): Boolean {
        val other = obj as? BlockPoint2D ?: return false
        return this.x == other.x && this.z == other.z
    }

    override fun toString(): String {
        return "BlockVector2($x, $z)"
    }
}