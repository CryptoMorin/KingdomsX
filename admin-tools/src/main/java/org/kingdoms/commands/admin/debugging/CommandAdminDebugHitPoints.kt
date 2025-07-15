package org.kingdoms.commands.admin.debugging

import com.cryptomorin.xseries.XMaterial
import com.cryptomorin.xseries.XTag
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.permissions.PermissionDefault
import org.kingdoms.admintools.AdminToolsLang
import org.kingdoms.commands.CommandContext
import org.kingdoms.commands.CommandResult
import org.kingdoms.commands.KingdomsCommand
import org.kingdoms.commands.admin.debugging.debug.CommandAdminDebug
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdAssertPlayer
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm
import org.kingdoms.utils.hash.EntityHashSet

@Cmd("hitpoints")
@CmdParent(CommandAdminDebug::class)
@CmdAssertPlayer
@CmdPerm(PermissionDefault.OP)
class CommandAdminDebugHitPoints : KingdomsCommand(), Listener {
    companion object {
        private val HITPOINTS: MutableSet<Player> = EntityHashSet
            .weakBuilder(Player::class.java)
            .onLeave { t, u -> t.remove(u) }
            .build()
    }

    override fun execute(context: CommandContext): CommandResult {
        val player = context.senderAsPlayer()

        val isToggled = HITPOINTS.contains(player)
        if (isToggled) {
            HITPOINTS.remove(player)
            context.sendMessage(AdminToolsLang.COMMAND_ADMIN_DEBUGGING_HITPOINTS_DISABLED)
        } else {
            HITPOINTS.add(player)
            context.sendMessage(AdminToolsLang.COMMAND_ADMIN_DEBUGGING_HITPOINTS_ENABLED)
        }

        return CommandResult.SUCCESS
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDamage(event: EntityDamageByEntityEvent) {
        val finalDamage = event.finalDamage

        if (HITPOINTS.contains(event.damager)) {
            val damager = event.damager as Player
            val victim = event.entity

            AdminToolsLang.COMMAND_ADMIN_DEBUGGING_HITPOINTS_INDICATOR_DEALT.sendMessage(
                damager,
                "damage", finalDamage,
                "entity", victim.customName ?: victim.name,
                "tool", damageTool(damager),
                "cause", event.cause
            )
        } else if (HITPOINTS.contains(event.entity)) {
            val damager = event.damager
            val victim = event.entity as Player

            AdminToolsLang.COMMAND_ADMIN_DEBUGGING_HITPOINTS_INDICATOR_RECEIVED.sendMessage(
                victim,
                "damage", finalDamage,
                "entity", damager.customName ?: damager.name,
                "tool", damageTool(damager),
                "cause", event.cause
            )
        }
    }

    private fun damageTool(damager: Entity): String {
        return if (damager is Player) {
            val itemInHand = XMaterial.matchXMaterial(damager.inventory.itemInMainHand.type)

            if (XTag.AIR.isTagged(itemInHand)) {
                "your fists"
            } else {
                itemInHand.toString()
            }
        } else if (damager is InventoryHolder) {
            damager.inventory.getItem(0)?.type?.toString() ?: "[BASE DAMAGE]"
        } else {
            "[BASE DAMAGE]"
        }
    }
}