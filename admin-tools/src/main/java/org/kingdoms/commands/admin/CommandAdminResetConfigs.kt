package org.kingdoms.commands.admin

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.permissions.PermissionDefault
import org.kingdoms.admintools.AdminToolsLang
import org.kingdoms.commands.CommandContext
import org.kingdoms.commands.CommandResult
import org.kingdoms.commands.CommandResult.*
import org.kingdoms.commands.KingdomsCommand
import org.kingdoms.commands.KingdomsParentCommand
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm
import org.kingdoms.config.managers.ConfigResetFactory
import org.kingdoms.locale.KingdomsLang
import org.kingdoms.main.KLogger
import org.kingdoms.main.Kingdoms
import org.kingdoms.main.KingdomsGlobalsCenter
import org.kingdoms.utils.fs.FSUtil
import java.time.Duration

@Cmd("resetConfigs")
@CmdParent(CommandAdmin::class)
@CmdPerm(PermissionDefault.OP)
class CommandAdminResetConfigs : KingdomsCommand(), Listener {
    companion object {
        private var ACCEPT_COOLDOWN = 0L
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerJoin(event: AsyncPlayerPreLoginEvent) {
        if (!ConfigResetFactory.RESETTING) return
        event.disallow(
            AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
            "${ChatColor.RED} Server is currently whitelisted by kingdoms"
        )
    }

    override fun execute(context: CommandContext): CommandResult {
        if (context.getMessageReceiver() is Player) {
            context.sendError(AdminToolsLang.COMMAND_ADMIN_RESETCONFIGS_CONSOLE_ONLY)
            return FAILED
        }

        if (ConfigResetFactory.RESETTING) {
            context.sendError(AdminToolsLang.COMMAND_ADMIN_RESETCONFIGS_ALREADY)
            return FAILED
        }

        if (ACCEPT_COOLDOWN == 0L || Duration.ofSeconds(5)
                .minus(Duration.ofMillis(System.currentTimeMillis() - ACCEPT_COOLDOWN)).isNegative
        ) {
            ACCEPT_COOLDOWN = System.currentTimeMillis()
            context.sendError(AdminToolsLang.COMMAND_ADMIN_RESETCONFIGS_CONFIRM)
            return PARTIAL
        }

        KingdomsGlobalsCenter.get().set("reset-configs-on-next-start", true)
        KingdomsGlobalsCenter.queryUpdate();
        context.sendMessage(AdminToolsLang.COMMAND_ADMIN_RESETCONFIGS_REQUESTED)
        Kingdoms.taskScheduler().async().delayed(Duration.ofSeconds(10), { ->
            KLogger.info("Shutting down...")
            Bukkit.shutdown()
        })
        return SUCCESS
    }
}