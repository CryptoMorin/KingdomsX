package org.kingdoms.services.maps.abstraction.markers

import org.kingdoms.server.location.Vector3

interface Marker {
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("id")
    val id: Any

    fun delete()
}

class MarkerZoom private constructor(val min: Number?, val max: Number?) {
    fun percentify(minValue: Number, maxValue: Number): MarkerZoom {
        return of(
            if (min === null || min.toDouble() <= 0) min else valueFromPercent(
                minValue.toDouble(),
                maxValue.toDouble(),
                min.toDouble()
            ),
            if (max === null || max.toDouble() <= 0) max else valueFromPercent(
                minValue.toDouble(),
                maxValue.toDouble(),
                max.toDouble()
            )
        )
    }

    companion object {
        @JvmStatic fun of(min: Number?, max: Number?) = MarkerZoom(min, max)

        @JvmStatic fun valueFromPercent(min: Double, max: Double, percent: Double): Double {
            // Finds the value (in the range [min, max]) from the given percentage (from 0.0 to 100.0)
            return min + (max - min) * (percent / 100)
        }
    }
}

interface LandMarker : Marker {
    fun setSettings(settings: LandMarkerSettings)
}

interface IconMarker : Marker {
    val location: Vector3
    fun setSettings(settings: LandMarkerSettings)
}
