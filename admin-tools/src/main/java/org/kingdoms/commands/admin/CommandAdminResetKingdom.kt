package org.kingdoms.commands.admin

import org.bukkit.event.Listener
import org.bukkit.permissions.PermissionDefault
import org.kingdoms.admintools.AdminToolsLang
import org.kingdoms.commands.CommandContext
import org.kingdoms.commands.CommandResult
import org.kingdoms.commands.CommandTabContext
import org.kingdoms.commands.KingdomsCommand
import org.kingdoms.commands.KingdomsParentCommand
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm
import org.kingdoms.constants.group.Kingdom
import org.kingdoms.events.lands.UnclaimLandEvent
import org.kingdoms.events.members.LeaveReason
import org.kingdoms.server.permission.PermissionDefaultValue
import org.kingdoms.utils.internal.enumeration.EnumsKt
import org.kingdoms.utils.internal.enumeration.QuickEnumSet
import java.util.*

@Cmd("resetKingdom")
@CmdParent(CommandAdmin::class)
@CmdPerm(PermissionDefaultValue.OP)
class CommandAdminResetKingdom : KingdomsCommand() {
    /**
     * Using reflection will make this cleaner, however the ProGuard rules
     * will prevent us from working with the private fields effectively.
     * Maybe it's just better this way.
     *
     * Order of this enum is important and determines which action should be performed first.
     * Except grouped actions which are just psuedo actions and themselves do nothing.
     */
    private enum class KingdomDataComponent {
        EVERYTHING, // Handled by the set populator

        RESOURCE_POINTS({ resourcePoints.set(0) }),
        BANK({ bank.set(0) }),

        MEMBER_LIST({
            kingdomPlayers.filter { !it.rank!!.isKing }.forEach { player -> player.leaveKingdom(LeaveReason.ADMIN) }
        }),

        POWERUPS({ powerups.clear() }),
        MISC_UPGRADES({ miscUpgrades.clear() }),
        CHAMPION_UPGRADES({ championUpgrades.clear() }),

        NEXUS_VAULT({ nexusChest.original.clear() }),
        RELATIONSHIP_REQUESTS({ relationshipRequests.clear() }),
        RELATIONS({ relations.clear() }),
        ATTRIBUTES({ attributes.clear() }),

        LORE({ setLore(null, null) }),
        TAG({ tag = null }),

        NEXUS({ nexus = null }),
        HOME({ setHome(null, null) }),

        SHIELD({ shield.deactivate() }),
        LEVEL({ setLevel(1) }),
        MAILS({ mailIds.clear() }),
        BOOK({ book.clear() }),

        FLAG({ setFlag(null, null) }),
        BANNER({ setBanner(null, null) }),

        MAX_LANDS_MODIFIER({ maxLandsModifier = 0 }),
        NATION({ if (hasNation()) leaveNation(LeaveReason.ADMIN) }),
        CHALLENGES({ challenges.clear() }),
        INVITE_CODES({ inviteCodes.clear() }),
        NATION_INVITES({ nationInvites.clear() }),
        FLAGS({ getFlags().clear() }),

        UPGRADES(LEVEL, POWERUPS, MISC_UPGRADES, CHAMPION_UPGRADES),

        TURRETS({ lands.forEach { land -> land.turrets.forEach { turret -> turret.value.remove() } } }),
        STRUCTURES({ lands.forEach { land -> land.structures.forEach { turret -> turret.value.remove() } } }),
        UNCLAIM_LANDS({ if (landLocations.isNotEmpty()) unclaimIf(null, UnclaimLandEvent.Reason.ADMIN) { l -> true } }),
        LANDS(UNCLAIM_LANDS, TURRETS, STRUCTURES),

        LOGS({ getLogs().clear() }),
        STATISTICS({ statistics.clear() }),
        META_DATA({ metadata.clear() }),

        ;

        // We can't use EnumSets here because of enum initialization order.
        // https://bugs.openjdk.org/browse/JDK-8211749
        val effectiveFactors: Array<KingdomDataComponent>
        val handler: (Kingdom) -> Unit

        constructor(handler: Kingdom.() -> Unit) {
            this.handler = handler
            effectiveFactors = arrayOf(this)
        }

        constructor(vararg group: KingdomDataComponent) {
            this.handler = { throw UnsupportedOperationException("$name should not be executed") }
            effectiveFactors = arrayOf(this).plus(group)
        }
    }

    private fun getFactors(context: CommandContext, factors: List<String>): List<KingdomDataComponent> {
        val insensitiveFactors = factors.map { it.uppercase(Locale.ENGLISH) }.toHashSet()
        val everything = insensitiveFactors.remove(KingdomDataComponent.EVERYTHING.name)

        val universe = KingdomDataComponent::class.java.enumConstants
        val list = if (everything)
            QuickEnumSet.allOf(universe)
        else
            QuickEnumSet(universe)

        for (factor in insensitiveFactors) {
            val isNegative = factor.startsWith('-')

            val realName = if (isNegative || factor.startsWith('+')) factor.substring(1) else factor
            val factorObj = EnumsKt.nullableValueOf<KingdomDataComponent>(realName)
            context.`var`("component", realName)

            if (factorObj === null) {
                throw context.error(AdminToolsLang.COMMAND_ADMIN_RESETKINGDOM_UNKNOWN_FACTOR)
            }

            if (isNegative) {
                if (!everything) throw context.error(AdminToolsLang.COMMAND_ADMIN_RESETKINGDOM_INVALID_NEGATIVE_FACTOR)
                list.removeAll(factorObj.effectiveFactors)
            } else {
                if (everything) throw context.error(AdminToolsLang.COMMAND_ADMIN_RESETKINGDOM_INVALID_POSITIVE_FACTOR)
                list.addAll(factorObj.effectiveFactors)
            }
        }

        // Remove groups themselves.
        list.remove(KingdomDataComponent.EVERYTHING)
        list.removeIf { it.effectiveFactors.size > 1 }
        return list.sorted()
    }

    override fun execute(context: CommandContext): CommandResult {
        val kingdom = context.generalSelector<Kingdom>(false) ?: return CommandResult.FAILED
        val factorArgs = context.args.drop(context.nextArg())
        if (factorArgs.isEmpty()) throw context.wrongUsage()

        val factors = getFactors(context, factorArgs)

        factors.forEach {
            try {
                it.handler(kingdom)
            } catch (ex: Throwable) {
                ex.printStackTrace()
                context.`var`("component", it.name)
                return context.fail(AdminToolsLang.COMMAND_ADMIN_RESETKINGDOM_FAILED)
            }
        }
        context.sendMessage(AdminToolsLang.COMMAND_ADMIN_RESETKINGDOM_DONE)
        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): List<String?> {
        return context.tabCompleteGeneralSelector<Kingdom>(false, false, null)
            .thenRemaining {
                context.suggest(
                    context.lastArgumentPosition(),
                    KingdomDataComponent.values().map { it.name })
            }
    }
}