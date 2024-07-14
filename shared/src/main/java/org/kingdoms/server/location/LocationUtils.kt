package org.kingdoms.server.location

import org.kingdoms.utils.internal.numbers.NumberExtensions.squared
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.sqrt

internal object LocationUtils {
    const val PII = 2 * Math.PI

    fun toBlock(num: Double): Int {
        val floor = num.toInt()
        return if (floor.toDouble() == num) floor else floor - (java.lang.Double.doubleToRawLongBits(num) ushr 63).toInt()
    }

    fun fromDirection(vector: Vector3): Directional {
        /*
         * Sin = Opp / Hyp
         * Cos = Adj / Hyp
         * Tan = Opp / Adj
         *
         * x = -Opp
         * z = Adj
         */
        val x: Double = vector.x
        val y: Double = vector.y
        val z: Double = vector.z

        if (x == 0.0 && z == 0.0) {
            return Directional.of(
                yaw = 0f,
                pitch = if (y > 0) -90f else 90f
            )
        }

        val theta = atan2(-x, z)
        val yaw = Math.toDegrees((theta + PII) % PII).toFloat()

        val x2: Double = x.squared()
        val z2: Double = z.squared()
        val xz = sqrt(x2 + z2)
        val pitch = Math.toDegrees(atan(-y / xz)).toFloat()

        return Directional.of(yaw, pitch)
    }
}
