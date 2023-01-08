@file:Suppress("ConvertSecondaryConstructorToPrimary")

package org.kingdoms.peacetreaties.data

import org.kingdoms.constants.group.model.logs.AuditLog
import org.kingdoms.constants.group.model.logs.AuditLogProvider
import org.kingdoms.constants.group.model.logs.StandardAuditLogProvider
import org.kingdoms.constants.land.abstraction.data.DeserializationContext
import org.kingdoms.constants.land.abstraction.data.SerializationContext
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.locale.provider.MessageBuilder
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getReceivedPeaceTreaties
import org.kingdoms.utils.internal.FastUUID
import java.time.Duration

abstract class LogPeaceTreaty() : AuditLog() {
    var peaceTreaty: PeaceTreaty? = null
        get() = field?.let { it.victimKingdom.getReceivedPeaceTreaties()[it.proposerKingdomId] ?: field }

    constructor(peaceTreaty: PeaceTreaty) : this() {
        this.peaceTreaty = peaceTreaty
    }

    override fun deserialize(context: DeserializationContext) {
        super.deserialize(context)
        val json = context.json

        val proposerKingdomId = FastUUID.fromString(json["proposerKingdom"].asString)
        val player = FastUUID.fromString(json["player"].asString)
        val victimKingdom = FastUUID.fromString(json["victimKingdom"].asString)
        val duration = json["duration"].asLong

        this.peaceTreaty = PeaceTreaty(proposerKingdomId, victimKingdom, 0, time, Duration.ofMillis(duration), player)
    }

    override fun serialize(context: SerializationContext) {
        super.serialize(context)
        val json = context.json
        val peaceTreaty = this.peaceTreaty!!

        json.addProperty("player", FastUUID.toString(peaceTreaty.requesterPlayerID))
        json.addProperty("proposerKingdom", FastUUID.toString(peaceTreaty.proposerKingdomId))
        json.addProperty("victimKingdom", FastUUID.toString(peaceTreaty.victimKingdomId))
        json.addProperty("duration", peaceTreaty.duration.toMillis())
    }

    override fun addEdits(builder: MessageBuilder) {
        super.addEdits(builder)
        builder.inheritPlaceholders(peaceTreaty!!.placeholderContextProvider)
    }
}

class LogPeaceTreatyReceived : LogPeaceTreaty {
    constructor() : super()
    constructor(peaceTreaty: PeaceTreaty) : super(peaceTreaty)

    companion object {
        @JvmField
        val PROVIDER = StandardAuditLogProvider(Namespace("PeaceTreaties", "RECEIVED")) { LogPeaceTreatyReceived() }
    }

    override fun getProvider(): AuditLogProvider = PROVIDER
}

class LogPeaceTreatySent : LogPeaceTreaty {
    constructor() : super()
    constructor(peaceTreaty: PeaceTreaty) : super(peaceTreaty)

    companion object {
        @JvmField
        val PROVIDER = StandardAuditLogProvider(Namespace("PeaceTreaties", "SENT")) { LogPeaceTreatySent() }
    }

    override fun getProvider(): AuditLogProvider = PROVIDER
}