package org.kingdoms.peacetreaties.data

import org.kingdoms.constants.base.KeyedKingdomsObject
import org.kingdoms.constants.group.Group
import org.kingdoms.constants.group.Kingdom
import org.kingdoms.constants.land.abstraction.data.DeserializationContext
import org.kingdoms.constants.land.abstraction.data.SerializationContext
import org.kingdoms.constants.metadata.KingdomMetadata
import org.kingdoms.constants.metadata.KingdomMetadataHandler
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.data.database.dataprovider.SectionCreatableDataSetter
import org.kingdoms.data.database.dataprovider.SectionableDataGetter
import org.kingdoms.locale.placeholders.EnumKingdomsPlaceholderTranslator
import org.kingdoms.locale.placeholders.KingdomsPlaceholderTranslator
import org.kingdoms.locale.placeholders.PlaceholderTranslator
import org.kingdoms.peacetreaties.PeaceTreatiesAddon
import org.kingdoms.peacetreaties.data.WarPoint.Companion.getWarPoints
import org.kingdoms.peacetreaties.terms.TermRegistry
import org.kingdoms.utils.string.Strings
import java.time.Duration
import java.util.*
import java.util.function.Supplier

typealias PeaceTreatyMap = Map<UUID, PeaceTreaty>

class PeaceTreatyReceiverMeta(var peaceTreaties: PeaceTreatyMap) : KingdomMetadata {
    @Suppress("UNCHECKED_CAST")
    override var value: Any
        get() = peaceTreaties
        set(value) {
            this.peaceTreaties = value as PeaceTreatyMap
        }

    override fun toString(): String = "PeaceTreatyReceiverMeta[${Strings.associatedArrayMap(peaceTreaties)}]"

    override fun serialize(
        container: KeyedKingdomsObject<*>,
        context: SerializationContext<SectionCreatableDataSetter>
    ) {
        val provider = context.dataProvider

        provider.setMap(peaceTreaties) { proposerId, keyProvider, contract ->
            keyProvider.setUUID(proposerId)
            val valueProvider = keyProvider.getValueProvider().createSection()

            valueProvider["terms"].setMap(contract.terms) { termName, termKeyProvider, termValue ->
                termKeyProvider.setString(termName)
                termKeyProvider.getValueProvider().setMap(termValue.terms) { subtermName, subtermKeyProvider, subterm ->
                    subtermKeyProvider.setString(subtermName.asNormalizedString())
                    subterm.serialize(SerializationContext(subtermKeyProvider.getValueProvider().createSection()))
                }
            }

            with(valueProvider) {
                setUUID("requesterPlayer", contract.requesterPlayerID)
                setLong("duration", contract.duration.toMillis())
                setLong("started", contract.started)
                setLong("time", contract.sentTime)
            }
        }
    }

    override fun shouldSave(container: KeyedKingdomsObject<*>): Boolean = peaceTreaties.isNotEmpty()
}

class PeaceTreatyProposedMeta(var peaceTreaties: MutableSet<UUID>) : KingdomMetadata {
    @Suppress("UNCHECKED_CAST")
    override var value: Any
        get() = peaceTreaties
        set(value) {
            peaceTreaties = value as MutableSet<UUID>
        }

    override fun serialize(
        container: KeyedKingdomsObject<*>,
        context: SerializationContext<SectionCreatableDataSetter>
    ) {
        context.dataProvider.setCollection(peaceTreaties) { elementProvider, id -> elementProvider.setUUID(id) }
    }

    override fun shouldSave(container: KeyedKingdomsObject<*>): Boolean = peaceTreaties.isNotEmpty()
}

@Suppress("unused")
enum class PeaceTreatiesPlaceholder(override val default: Any, override val translator: PlaceholderTranslator) :
    EnumKingdomsPlaceholderTranslator {
    TOTAL_WAR_POINTS(0, KingdomsPlaceholderTranslator.ofKingdom { x -> x.getWarPoints().values.sum() }),
    WAR_POINTS(0, lambda@{ ctx ->
        val kingdom = ctx.getKingdom() ?: return@lambda null
        val otherKingdom = ctx.getOtherKingdom() ?: return@lambda null
        return@lambda kingdom.getWarPoints(otherKingdom)
    })
    ;

    override var configuredDefaultValue: Any? = null

    init {
        KingdomsPlaceholderTranslator.register(this)
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
            val data: KingdomMetadata =
                this.metadata[PeaceTreatyReceiverMetaHandler.INSTANCE] ?: return Collections.emptyMap()
            return Collections.unmodifiableMap(data.value as PeaceTreatyMap)
        }

        @JvmStatic
        fun Kingdom.getContractWith(other: Kingdom): PeaceTreaty? {
            return this.getReceivedPeaceTreaties().values.find { x -> x.proposerKingdomId == other.id }
                ?: other.getReceivedPeaceTreaties().values.find { x -> x.proposerKingdomId == this.id }
        }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T> initializeMeta(
            kingdom: Kingdom,
            metadataHandler: KingdomMetadataHandler,
            default: Supplier<KingdomMetadata>,
        ): T {
            var metadata: KingdomMetadata? = kingdom.metadata[metadataHandler]
            if (metadata == null) {
                metadata = default.get()
                kingdom.metadata[metadataHandler] = metadata
            }
            return metadata.value as T
        }

        @JvmStatic
        fun Kingdom.getAcceptedPeaceTreaty(): PeaceTreaty? =
            this.getReceivedPeaceTreaties().values.find { p -> p.isAccepted }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun Kingdom.getProposedPeaceTreaties(): PeaceTreatyMap {
            val data = this.metadata[PeaceTreatyProposerMetaHandler.INSTANCE] ?: return Collections.emptyMap()

            val set: MutableSet<UUID> = data.value as MutableSet<UUID>
            val contractMap: MutableMap<UUID, PeaceTreaty> = hashMapOf()

            val iter = set.iterator()
            while (iter.hasNext()) {
                val uuid = iter.next()
                val other = Kingdom.getKingdom(uuid) ?: continue
                val realContract = other.getReceivedPeaceTreaties()[this.key]
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

class PeaceTreatyReceiverMetaHandler private constructor() :
    KingdomMetadataHandler(Namespace("PeaceTreaties", "RECEIVED")) {
    @Suppress("LABEL_NAME_CLASH")
    override fun deserialize(
        container: KeyedKingdomsObject<*>,
        context: DeserializationContext<SectionableDataGetter>,
    ): KingdomMetadata {
        return PeaceTreatyReceiverMeta(context.dataProvider.asMap(hashMapOf()) { map, key, value ->
            val proposerID = key.asUUID()
            val receiverID = (container as Group).key

            val started = value["started"].asLong()
            val duration = Duration.ofMillis(value["duration"].asLong())
            val sentTime = value["time"].asLong()
            val requesterPlayer = value["requesterPlayer"].asUUID()

            val contract = PeaceTreaty(proposerID, receiverID, started, sentTime, duration, requesterPlayer)

            val termsObj: Map<Void, Void> = value["terms"].asMap(hashMapOf()) { _, termKey, termValue ->
                val grouping = TermRegistry.getTermGroupings()[termKey.asString()] ?: return@asMap
                val subTerms: Map<Void, Void> = termValue.asMap(hashMapOf()) { _, subTermKey, subTermvalue ->
                    val subTermProvider =
                        PeaceTreatiesAddon.get().termRegistry.getRegistered(Namespace.fromString(subTermKey.asString()!!))
                            ?: return@asMap
                    val constructed = subTermProvider.construct()

                    constructed.deserialize(DeserializationContext(subTermvalue))
                    contract.addOrCreateTerm(grouping, constructed)
                }
            }

            map[proposerID!!] = contract
        })
    }

    companion object {
        @JvmField
        val INSTANCE = PeaceTreatyReceiverMetaHandler()
    }
}

class PeaceTreatyProposerMetaHandler private constructor() :
    KingdomMetadataHandler(Namespace("PeaceTreaties", "PROPOSED")) {
    override fun deserialize(
        container: KeyedKingdomsObject<*>,
        context: DeserializationContext<SectionableDataGetter>,
    ): PeaceTreatyProposedMeta {
        return PeaceTreatyProposedMeta(context.dataProvider.asCollection(hashSetOf()) { c, x -> c.add(x.asUUID()!!) })
    }

    companion object {
        @JvmField
        val INSTANCE: PeaceTreatyProposerMetaHandler = PeaceTreatyProposerMetaHandler()
    }
}