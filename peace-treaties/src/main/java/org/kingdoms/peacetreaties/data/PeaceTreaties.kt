package org.kingdoms.peacetreaties.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import org.kingdoms.adapters.KingdomsGson
import org.kingdoms.constants.group.Group
import org.kingdoms.constants.group.Kingdom
import org.kingdoms.constants.land.abstraction.data.DeserializationContext
import org.kingdoms.constants.land.abstraction.data.SerializationContext
import org.kingdoms.constants.metadata.KingdomMetadata
import org.kingdoms.constants.metadata.KingdomMetadataHandler
import org.kingdoms.constants.metadata.KingdomsObject
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.locale.compiler.placeholders.KingdomsPlaceholder
import org.kingdoms.locale.compiler.placeholders.PlaceholderTranslator
import org.kingdoms.peacetreaties.PeaceTreatiesAddon
import org.kingdoms.peacetreaties.data.WarPoint.Companion.getWarPoints
import org.kingdoms.peacetreaties.terms.TermRegistry
import org.kingdoms.utils.internal.FastUUID
import java.time.Duration
import java.util.*

typealias PeaceTreatyMap = Map<UUID, PeaceTreaty>

class PeaceTreatyReceiverMeta(var peaceTreaties: PeaceTreatyMap) : KingdomMetadata {
    override fun getValue(): PeaceTreatyMap = peaceTreaties
    override fun setValue(p0: Any) {
        @Suppress("UNCHECKED_CAST")
        this.peaceTreaties = p0 as PeaceTreatyMap
    }

    override fun serialize(container: KingdomsObject, jsonSerializationContext: JsonSerializationContext): JsonElement {
        val contractsObj = JsonObject()

        for (contractEntry in peaceTreaties) {
            val contractObj = JsonObject()
            val contract = contractEntry.value
            val terms = JsonObject()

            for (termGrouping in contract.terms) {
                val termGroupingObj = JsonObject()

                for (term in termGrouping.value.terms) {
                    val termObj = JsonObject()
                    term.value.serialize(SerializationContext(termObj, jsonSerializationContext))
                    termGroupingObj.add(term.key.asNormalizedString(), termObj)
                }

                terms.add(termGrouping.key, termGroupingObj)
            }

            with(contractObj) {
                addProperty("requesterPlayer", FastUUID.toString(contract.requesterPlayerID))
                addProperty("duration", contract.duration.toMillis())
                addProperty("started", contract.started)
                addProperty("time", contract.sentTime)
                add("terms", terms)
            }

            val key = FastUUID.toString(contract.proposerKingdomId)
            contractsObj.add(key, contractObj)
        }

        return contractsObj
    }

    override fun shouldSave(container: KingdomsObject): Boolean = peaceTreaties.isNotEmpty()
}

class PeaceTreatyProposedMeta(var peaceTreaties: MutableSet<UUID>) : KingdomMetadata {
    override fun getValue(): MutableSet<UUID> = peaceTreaties
    override fun setValue(p0: Any) {
        @Suppress("UNCHECKED_CAST")
        this.peaceTreaties = p0 as MutableSet<UUID>
    }

    override fun serialize(container: KingdomsObject, jsonSerializationContext: JsonSerializationContext): JsonElement {
        return jsonSerializationContext.serialize(peaceTreaties, KingdomsGson.UUID_SET)
    }

    override fun shouldSave(container: KingdomsObject): Boolean = peaceTreaties.isNotEmpty()
}

@Suppress("unused")
enum class PeaceTreatiesPlaceholder(default: Any, translator: PlaceholderTranslator) {
    TOTAL_WAR_POINTS(0, KingdomsPlaceholder.ofKingdom { x -> x.getWarPoints().values.sum() }),
    WAR_POINTS(0, lambda@{ ctx ->
        val kingdom = ctx.getKingdom() ?: return@lambda null
        val otherKingdom = ctx.getOtherKingdom() ?: return@lambda null
        return@lambda kingdom.getWarPoints(otherKingdom)
    })
    ;

    init {
        KingdomsPlaceholder.of(this.name.lowercase(), default, translator)
    }

    companion object {
        @JvmStatic
        fun init() {
        }
    }
}

class PeaceTreaties {
    companion object {
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun Kingdom.getReceivedPeaceTreaties(): PeaceTreatyMap {
            var data: KingdomMetadata? = this.metadata[PeaceTreatyReceiverMetaHandler.INSTANCE]
            if (data == null) {
                data = PeaceTreatyReceiverMeta(hashMapOf())
                this.metadata[PeaceTreatyReceiverMetaHandler.INSTANCE] = data
            }

            return Collections.unmodifiableMap(data.value as PeaceTreatyMap)
        }

        @JvmStatic
        fun Kingdom.getAcceptedPeaceTreaty(): PeaceTreaty? =
            this.getReceivedPeaceTreaties().values.find { p -> p.isAccepted }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun Kingdom.getProposedPeaceTreaties(): PeaceTreatyMap {
            var data = this.metadata[PeaceTreatyProposerMetaHandler.INSTANCE]
            if (data == null) {
                data = PeaceTreatyProposedMeta(hashSetOf())
                this.metadata[PeaceTreatyProposerMetaHandler.INSTANCE] = data
            }

            val set: MutableSet<UUID> = data.value as MutableSet<UUID>
            val contractMap: MutableMap<UUID, PeaceTreaty> = hashMapOf()

            val iter = set.iterator()
            while (iter.hasNext()) {
                val uuid = iter.next()
                val other = Kingdom.getKingdom(uuid) ?: continue
                val realContract = other.getReceivedPeaceTreaties()[this.id]
                if (realContract == null) {
                    iter.remove()
                    continue
                }

                contractMap[uuid] = realContract
            }

            return Collections.unmodifiableMap(contractMap)
        }

        @JvmStatic
        fun Kingdom.getPeaceTreaties(): Collection<PeaceTreaty> {
            return Collections.unmodifiableCollection(getReceivedPeaceTreaties().values.plus(getProposedPeaceTreaties().values))
        }
    }
}

class PeaceTreatyReceiverMetaHandler private constructor() : KingdomMetadataHandler(Namespace("PeaceTreaties", "RECEIVED")) {
    override fun deserialize(container: KingdomsObject, jsonElement: JsonElement, jsonDeserializationContext: JsonDeserializationContext): PeaceTreatyReceiverMeta {
        val contractsObj = jsonElement.asJsonObject
        val contracts = hashMapOf<UUID, PeaceTreaty>()

        for (contractObj in contractsObj.entrySet()) {
            val proposerID = FastUUID.fromString(contractObj.key)
            val receiverID = (container as Group).id

            val data = contractObj.value.asJsonObject
            val started = data["started"].asLong
            val duration = Duration.ofMillis(data["duration"].asLong)
            val sentTime = data["time"].asLong
            val requesterPlayer = FastUUID.fromString(data["requesterPlayer"].asString)

            val contract = PeaceTreaty(proposerID, receiverID, started, sentTime, duration, requesterPlayer)

            val termsObj = data["terms"].asJsonObject

            for (termEntry in termsObj.entrySet()) {
                val termName = termEntry.key
                val term = termEntry.value
                val grouping = TermRegistry.getTermGroupings()[termName] ?: continue
                val subTerms = term.asJsonObject
                for (subTerm in subTerms.entrySet()) {
                    val subTermName = subTerm.key
                    val subTermProvider = PeaceTreatiesAddon.get().termRegistry.getRegistered(Namespace.fromString(subTermName)) ?: continue
                    val subTermObj = subTerm.value
                    val constructed = subTermProvider.construct()

                    constructed.deserialize(DeserializationContext(subTermObj.asJsonObject, jsonDeserializationContext))
                    contract.addOrCreateTerm(grouping, constructed)
                }
            }

            contracts[proposerID] = contract
        }

        return PeaceTreatyReceiverMeta(contracts)
    }

    companion object {
        @JvmField
        val INSTANCE = PeaceTreatyReceiverMetaHandler()
    }
}

class PeaceTreatyProposerMetaHandler private constructor() : KingdomMetadataHandler(Namespace("PeaceTreaties", "PROPOSED")) {
    override fun deserialize(container: KingdomsObject, jsonElement: JsonElement, jsonDeserializationContext: JsonDeserializationContext): PeaceTreatyProposedMeta {
        return PeaceTreatyProposedMeta(jsonDeserializationContext.deserialize(jsonElement, KingdomsGson.UUID_SET))
    }

    companion object {
        @JvmField
        val INSTANCE = PeaceTreatyProposerMetaHandler()
    }
}