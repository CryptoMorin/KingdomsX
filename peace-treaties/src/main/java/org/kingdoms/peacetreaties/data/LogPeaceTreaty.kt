@file:Suppress("ConvertSecondaryConstructorToPrimary")

package org.kingdoms.peacetreaties.data

import org.kingdoms.constants.group.model.logs.AuditLog
import org.kingdoms.constants.group.model.logs.AuditLogProvider
import org.kingdoms.constants.group.model.logs.StandardAuditLogProvider
import org.kingdoms.constants.land.abstraction.data.DeserializationContext
import org.kingdoms.constants.land.abstraction.data.SerializationContext
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.data.database.dataprovider.SectionableDataGetter
import org.kingdoms.data.database.dataprovider.SectionableDataSetter
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getReceivedPeaceTreaties
import java.time.Duration

abstract class LogPeaceTreaty() : AuditLog() {
    var peaceTreaty: PeaceTreaty? = null
        get() = field?.let { it.victimKingdom.getReceivedPeaceTreaties()[it.proposerKingdomId] ?: field }

    constructor(peaceTreaty: PeaceTreaty) : this() {
        this.peaceTreaty = peaceTreaty
    }

    override fun deserialize(context: DeserializationContext<SectionableDataGetter>) {
        super.deserialize(context)
        val json = context.dataProvider

        val proposerKingdomId = json["proposerKingdom"].asUUID()
        val player = json["player"].asUUID()
        val victimKingdom = json["victimKingdom"].asUUID()
        val duration = json["duration"].asLong()

        this.peaceTreaty = PeaceTreaty(proposerKingdomId, victimKingdom, 0, time, Duration.ofMillis(duration), player)
    }

    override fun serialize(context: SerializationContext<SectionableDataSetter>) {
        super.serialize(context)
        val json = context.dataProvider
        val peaceTreaty = this.peaceTreaty!!

        json.setUUID("player", peaceTreaty.requesterPlayerID)
        json.setUUID("proposerKingdom", peaceTreaty.proposerKingdomId)
        json.setUUID("victimKingdom", peaceTreaty.victimKingdomId)
        json.setLong("duration", peaceTreaty.duration.toMillis())
    }

    override fun addEdits(builder: MessagePlaceholderProvider) {
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