package org.kingdoms.peacetreaties.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import org.kingdoms.constants.group.Kingdom
import org.kingdoms.constants.metadata.KingdomMetadata
import org.kingdoms.constants.metadata.KingdomMetadataHandler
import org.kingdoms.constants.metadata.KingdomsObject
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.locale.compiler.placeholders.PlaceholderContextBuilder
import org.kingdoms.peacetreaties.config.PeaceTreatyConfig
import org.kingdoms.utils.MathUtils
import org.kingdoms.utils.internal.FastUUID
import java.util.*

typealias WarPoints = MutableMap<UUID, Double>

class WarPointsMeta(var warPoints: WarPoints) : KingdomMetadata {
    override fun getValue(): WarPoints = warPoints
    override fun setValue(p0: Any) {
        @Suppress("UNCHECKED_CAST")
        this.warPoints = p0 as WarPoints
    }

    override fun serialize(container: KingdomsObject, jsonSerializationContext: JsonSerializationContext): JsonElement {
        val obj = JsonObject()

        for (warPoint in warPoints) {
            obj.addProperty(FastUUID.toString(warPoint.key), warPoint.value)
        }

        return obj
    }

    override fun shouldSave(container: KingdomsObject): Boolean = warPoints.isNotEmpty()
}

class WarPoint {
    companion object {
        @JvmStatic
        fun Kingdom.getWarPoints(): WarPoints {
            val meta = this.metadata[WarPointsMetaHandler.INSTANCE]
            return if (meta == null) {
                val data: WarPoints = HashMap()
                this.metadata[WarPointsMetaHandler.INSTANCE] = WarPointsMeta(data)
                data
            } else {
                (meta as WarPointsMeta).warPoints
            }
        }

        @JvmStatic
        fun Kingdom.getWarPoints(other: Kingdom): Double = getWarPoints()[other.id] ?: 0.0

        @JvmStatic
        fun Kingdom.hasWarPoints(other: Kingdom, amount: Double): Boolean = getWarPoints(other) >= amount

        /**
         * Sets war points ignoring the max war points limit.
         */
        @JvmStatic
        fun Kingdom.setWarPoints(other: Kingdom, amount: Double) {
            val meta = getWarPoints()
            meta[other.id] = amount
        }

        @JvmStatic
        fun Kingdom.getMaxWarPoints(other: Kingdom): Double =
            MathUtils.eval(
                PeaceTreatyConfig.WAR_POINTS_MAX.manager.mathExpression,
                PlaceholderContextBuilder().withContext(this).other(other)
            )

        /**
         * Adds war points respecting the max war points.
         * If you want to bypass this limit use [setWarPoints] instead.
         */
        @JvmStatic
        fun Kingdom.addWarPoints(other: Kingdom, amount: Double): Double {
            val maxWarPoints = getMaxWarPoints(other)
            return getWarPoints().compute(other.id) { _, v -> maxWarPoints.coerceAtMost(if (v == null) amount else v + amount) }!!
        }
    }
}

class WarPointsMetaHandler private constructor() : KingdomMetadataHandler(Namespace("PeaceTreaties", "WAR_POINTS")) {
    override fun deserialize(container: KingdomsObject, jsonElement: JsonElement, jsonDeserializationContext: JsonDeserializationContext): WarPointsMeta {
        val obj = jsonElement.asJsonObject
        val warPoints: WarPoints = hashMapOf()

        for (warPointEntry in obj.entrySet()) {
            val kingdomId = FastUUID.fromString(warPointEntry.key)
            val points = warPointEntry.value.asDouble
            warPoints[kingdomId] = points
        }

        return WarPointsMeta(warPoints)
    }

    companion object {
        @JvmField
        val INSTANCE = WarPointsMetaHandler()
    }
}