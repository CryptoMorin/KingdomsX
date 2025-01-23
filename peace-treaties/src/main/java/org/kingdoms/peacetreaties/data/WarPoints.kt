package org.kingdoms.peacetreaties.data

import org.kingdoms.constants.base.KeyedKingdomsObject
import org.kingdoms.constants.group.Kingdom
import org.kingdoms.constants.land.abstraction.data.DeserializationContext
import org.kingdoms.constants.land.abstraction.data.SerializationContext
import org.kingdoms.constants.metadata.KingdomMetadata
import org.kingdoms.constants.metadata.KingdomMetadataHandler
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.data.database.dataprovider.SectionCreatableDataSetter
import org.kingdoms.data.database.dataprovider.SectionableDataGetter
import org.kingdoms.locale.placeholders.context.PlaceholderContextBuilder
import org.kingdoms.peacetreaties.config.PeaceTreatyConfig
import org.kingdoms.utils.MathUtils
import java.util.*

typealias WarPoints = MutableMap<UUID, Double>

class WarPointsMeta(var warPoints: WarPoints) : KingdomMetadata {
    @Suppress("UNCHECKED_CAST")
    override var value: Any
        get() = warPoints
        set(value) {
            this.warPoints = value as WarPoints
        }

    override fun serialize(
        container: KeyedKingdomsObject<*>,
        context: SerializationContext<SectionCreatableDataSetter>
    ) {
        context.dataProvider.setMap(warPoints) { key, keyProvider, value ->
            keyProvider.setUUID(key)
            keyProvider.getValueProvider().setDouble(value)
        }
    }

    override fun shouldSave(container: KeyedKingdomsObject<*>): Boolean = warPoints.isNotEmpty()
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
        fun Kingdom.getWarPoints(other: Kingdom): Double = getWarPoints()[other.key] ?: 0.0

        @JvmStatic
        fun Kingdom.hasWarPoints(other: Kingdom, amount: Double): Boolean = getWarPoints(other) >= amount

        /**
         * Sets war points ignoring the max war points limit.
         */
        @JvmStatic
        fun Kingdom.setWarPoints(other: Kingdom, amount: Double) {
            val meta = getWarPoints()
            meta[other.key] = amount
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
            return getWarPoints().compute(other.key) { _, v -> maxWarPoints.coerceAtMost(if (v == null) amount else v + amount) }!!
        }
    }
}

class WarPointsMetaHandler private constructor() : KingdomMetadataHandler(Namespace("PeaceTreaties", "WAR_POINTS")) {
    override fun deserialize(
        container: KeyedKingdomsObject<*>,
        context: DeserializationContext<SectionableDataGetter>,
    ): KingdomMetadata {
        return WarPointsMeta(context.dataProvider.asMap(hashMapOf()) { map, key, value ->
            map[key.asUUID()!!] = value.asDouble()
        })
    }

    companion object {
        @JvmField
        val INSTANCE = WarPointsMetaHandler()
    }
}