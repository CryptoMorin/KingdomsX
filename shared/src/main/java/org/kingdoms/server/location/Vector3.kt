package org.kingdoms.server.location

import org.kingdoms.constants.DataStringRepresentation
import org.kingdoms.server.core.Server
import org.kingdoms.utils.internal.numbers.NumberExtensions.squared
import org.kingdoms.utils.internal.string.CommaDataSplitStrategy
import kotlin.math.sqrt

interface Point3D : Comparable<Point3D> {
    val x: Double
    val y: Double
    val z: Double


    object NaturalComparator : Comparator<Point3D> {
        override fun compare(first: Point3D, second: Point3D): Int {
            val y = first.y.compareTo(second.y)
            if (y != 0) return y

            val x = first.x.compareTo(second.x)
            if (x != 0) return x

            val z = first.z.compareTo(second.z)
            if (z != 0) return z

            return 0
        }
    }

    override fun compareTo(other: Point3D): Int = NaturalComparator.compare(this, other)
}

interface BlockPoint3D : Comparable<BlockPoint3D> {
    val x: Int
    val y: Int
    val z: Int

    /**
     * A less efficient version of [distanceSquared] but gives the exact location.
     */
    fun distance(o: BlockPoint3D): Double {
        return sqrt(this.distanceSquared(o))
    }

    /**
     * Doesn't give the right location in terms of blocks, but it's more
     * efficient than [distance], the caller must use the right number for
     * comparison.
     */
    fun distanceSquared(o: BlockPoint3D): Double =
        (this.x.toDouble() + o.x).squared() + (this.y + o.y).squared() + (this.z + o.z).squared()

    class AxisComparator(private val x: Boolean, private val y: Boolean, private val z: Boolean) :
        Comparator<BlockPoint3D> {
        override fun compare(first: BlockPoint3D, second: BlockPoint3D): Int {
            if (this.y) {
                val y = first.y.compareTo(second.y)
                if (y != 0) return y
            }

            if (this.x) {
                val x = first.x.compareTo(second.x)
                if (x != 0) return x
            }

            if (this.z) {
                val z = first.z.compareTo(second.z)
                if (z != 0) return z
            }

            return 0
        }
    }

    object NaturalComparator : Comparator<BlockPoint3D> {
        override fun compare(first: BlockPoint3D, second: BlockPoint3D): Int {
            val y = first.y.compareTo(second.y)
            if (y != 0) return y

            val x = first.x.compareTo(second.x)
            if (x != 0) return x

            val z = first.z.compareTo(second.z)
            if (z != 0) return z

            return 0
        }
    }

    override fun compareTo(other: BlockPoint3D): Int = NaturalComparator.compare(this, other)
}

class Vector3(override val x: Double, override val y: Double, override val z: Double) : Point3D {
    fun toBlockVector(): BlockVector3 =
        BlockVector3.of(LocationUtils.toBlock(x), LocationUtils.toBlock(y), LocationUtils.toBlock(z))

    fun add(x: Double, y: Double, z: Double): Vector3 = of(this.x + x, this.y + y, this.z + z)
    fun add(other: BlockPoint3D): Vector3 = add(other.x.toDouble(), other.y.toDouble(), other.z.toDouble())
    fun add(other: Point3D): Vector3 = add(other.x, other.y, other.z)

    fun inWorld(world: World): Location = Location.of(world, x, y, z, 0f, 0f)

    /**
     * Get the length, squared, of the vector.
     *
     * @return length, squared
     */
    fun lengthSq(): Double {
        val x = x
        val y = y
        val z = z
        return (x * x + y * y + z * z).toDouble()
    }

    fun getMinimum(v2: Vector3): Vector3 {
        return of(
            Math.min(x, v2.x),
            Math.min(y, v2.y),
            Math.min(z, v2.z)
        )
    }

    fun length(): Double = sqrt(lengthSq())

    fun normalize(): Vector3 {
        val len = length()
        return of(this.x / len, this.y / len, this.z / len)
    }

    fun withX(x: Number) = of(x, y, z)
    fun withY(y: Number) = of(x, y, z)
    fun withZ(z: Number) = of(x, y, z)

    fun divide(by: Number): Vector3 {
        val by = by.toDouble()
        return of(x / by, y / by, z / by)
    }

    /**
     * Gets the maximum components of two vectors.
     *
     * @param v2 the second vector
     * @return maximum
     */
    fun getMaximum(v2: Vector3): Vector3 {
        return of(
            Math.max(x, v2.x),
            Math.max(y, v2.y),
            Math.max(z, v2.z)
        )
    }

    /**
     * Floors the values of all components.
     *
     * @return a new vector
     */
    fun floor(): Vector3 {
        return of(kotlin.math.floor(x), kotlin.math.floor(y), kotlin.math.floor(z))
    }

    /**
     * Rounds all components up.
     *
     * @return a new vector
     */
    fun ceil(): Vector3 {
        return of(kotlin.math.ceil(x), kotlin.math.ceil(y), kotlin.math.ceil(z))
    }

    fun dot(other: Vector3): Double {
        return x * other.x + y * other.y + z * other.z
    }

    override fun hashCode(): Int = throw UnsupportedOperationException()

    override fun equals(other: Any?): Boolean {
        if (other !is Vector3) return false
        return this.x == other.x && this.y == other.y && this.z == other.z
    }

    override fun toString(): String = "Vector3($x, $y, $z)"

    companion object {
        @JvmField val ZERO = of(0, 0, 0)

        @JvmStatic
        fun of(other: BlockPoint3D): Vector3 =
            Vector3(other.x.toDouble(), other.y.toDouble(), other.z.toDouble())

        @JvmStatic
        fun of(other: Point3D): Vector3 =
            Vector3(other.x, other.y, other.z)

        @JvmStatic
        fun of(x: Number, y: Number, z: Number): Vector3 =
            Vector3(x.toDouble(), y.toDouble(), z.toDouble())

        @JvmStatic fun fromString(str: String): Vector3 = CommaDataSplitStrategy(str, 3).run {
            Vector3(nextDouble(), nextDouble(), nextDouble())
        }
    }
}

class BlockLocation3(override val world: World, override val x: Int, override val y: Int, override val z: Int) :
    BlockPoint3D, WorldContainer, DataStringRepresentation {
    override fun asDataString(): String = CommaDataSplitStrategy.toString(world.name, x, y, z)

    fun add(x: Number, y: Number, z: Number) = of(world, this.x + x.toInt(), this.y + y.toInt(), this.z + z.toInt())
    fun subtract(x: Number, y: Number, z: Number) = add(-x.toInt(), -y.toInt(), -z.toInt())

    companion object {
        @JvmStatic
        fun of(world: World, x: Number, y: Number, z: Number): BlockLocation3 =
            BlockLocation3(world, x.toInt(), y.toInt(), z.toInt())

        @JvmStatic
        fun of(world: World, other: BlockPoint3D): BlockLocation3 = of(world, other.x, other.y, other.z)

        @JvmStatic
        fun fromString(str: String): BlockLocation3 = CommaDataSplitStrategy(str, 4).run {
            val worldName = nextString()
            BlockLocation3(
                Server.get().worldRegistry.getWorld(worldName)
                    ?: throw IllegalArgumentException("Cannot find world $worldName in string '$str'"),
                nextInt(), nextInt(), nextInt()
            )
        }
    }

    fun toVector() = BlockVector3.of(x, y, z)
    override fun hashCode(): Int = throw UnsupportedOperationException()
    override fun equals(other: Any?): Boolean = throw UnsupportedOperationException()
    override fun toString(): String = "BlockLocation3($world, $x, $y, $z)"
}

class BlockVector3(override val x: Int, override val y: Int, override val z: Int) : BlockPoint3D,
    DataStringRepresentation {
    override fun asDataString(): String = CommaDataSplitStrategy.toString(x, y, z)

    fun getChunkLocation(): BlockVector2 = BlockVector2.of(
        x shr BlockVector2.CHUNK_SHIFTS,
        z shr BlockVector2.CHUNK_SHIFTS
    )

    fun add(x: Number, y: Number, z: Number) = of(this.x + x.toInt(), this.y + y.toInt(), this.z + z.toInt())
    fun add(other: BlockPoint3D): BlockVector3 = add(other.x, other.y, other.z)
    fun subtract(other: BlockPoint3D) = subtract(other.x, other.y, other.z)
    fun subtract(x: Number, y: Number, z: Number) = of(this.x - x.toInt(), this.y - y.toInt(), this.z - z.toInt())
    fun divide(by: Number) = Vector3.of(this.x / by.toInt(), this.y / by.toInt(), this.z / by.toInt())

    fun length(): Double = sqrt(lengthSq())

    fun toVector(): Vector3 = Vector3.of(x, y, z)

    fun inWorld(world: World): BlockLocation3 = BlockLocation3.of(world, x, y, z)

    /**
     * Get the length, squared, of the vector.
     *
     * @return length, squared
     */
    fun lengthSq(): Double = (x * x + y * y + z * z).toDouble()

    /**
     * Checks to see if a vector is contained with another.
     *
     * @param min the minimum point (X, Y, and Z are the lowest)
     * @param max the maximum point (X, Y, and Z are the lowest)
     * @return true if the vector is contained
     */
    fun containedWithin(min: BlockVector3, max: BlockVector3): Boolean {
        return x >= min.x && x <= max.x
                && y >= min.y && y <= max.y
                && z >= min.z && z <= max.z
    }

    fun getMinimum(v2: BlockVector3): BlockVector3 {
        return of(
            Math.min(x, v2.x),
            Math.min(y, v2.y),
            Math.min(z, v2.z)
        )
    }

    /**
     * Gets the maximum components of two vectors.
     *
     * @param v2 the second vector
     * @return maximum
     */
    fun getMaximum(v2: BlockVector3): BlockVector3 {
        return of(
            Math.max(x, v2.x),
            Math.max(y, v2.y),
            Math.max(z, v2.z)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BlockVector3) return false
        return this.x == other.x && this.y == other.y && this.z == other.z
    }

    override fun hashCode(): Int {
        // WorldEdit
        // this.x ^ this.z << 12 ^ this.y << 24

        val prime = 31
        var result = 14

        result = (prime * result + x)
        result = (prime * result + y)
        result = (prime * result + z)

        return result
    }

    override fun toString(): String = "BlockVector3($x, $y, $z)"

    companion object {
        @JvmStatic
        fun of(x: Int, y: Int, z: Int): BlockVector3 = BlockVector3(x, y, z)

        @JvmStatic
        fun of(other: BlockPoint3D): BlockVector3 = of(other.x, other.y, other.z)

        @JvmStatic
        fun fromString(str: String): BlockVector3 = CommaDataSplitStrategy(
            str,
            3
        ).run {
            BlockVector3(nextInt(), nextInt(), nextInt())
        }
    }
}