package org.kingdoms.commands.admin

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.server.ServerCommandEvent
import org.bukkit.permissions.PermissionDefault
import org.kingdoms.admintools.AdminToolsLang
import org.kingdoms.commands.*
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm
import org.kingdoms.config.KingdomsConfig
import org.kingdoms.config.managers.ConfigWatcher
import org.kingdoms.data.centers.KingdomsDataCenter
import org.kingdoms.data.centers.KingdomsStartup
import org.kingdoms.events.general.GroupDisband
import org.kingdoms.events.items.KingdomItemRemoveContext
import org.kingdoms.gui.InteractiveGUIManager
import org.kingdoms.locale.KingdomsLang
import org.kingdoms.main.KLogger
import org.kingdoms.main.Kingdoms
import org.kingdoms.managers.chat.ChatInputManager
import org.kingdoms.server.permission.PermissionDefaultValue
import org.kingdoms.utils.fs.FSUtil
import java.time.Duration

@Cmd("purge")
@CmdParent(CommandAdmin::class)
@CmdPerm(PermissionDefaultValue.OP)
class CommandAdminPurge : KingdomsCommand(), Listener {
    companion object {
        private var ACCEPT_COOLDOWN = 0L
        private var PURGING = false
        private var DONE = false
        private var FULLY_LOADED = false

        init {
            KingdomsStartup.whenReady {
                Kingdoms.taskScheduler().async().delayed(Duration.ofSeconds(5)) { -> FULLY_LOADED = true }
            }
        }

        fun MutableList<Throwable>.tryThis(trying: () -> Unit) {
            try {
                trying()
            } catch (ex: Throwable) {
                this.add(ex)
            }
        }

        fun sanitizeStackTrace(throwable: Throwable): String {
            val builder = StringBuilder(1000)
            var error: Throwable = throwable

            // Usually the last error has the most information.
            while (error.cause != null) error = error.cause!!

            val stackTrace: Array<StackTraceElement> = error.stackTrace
            builder.append(error.message).append('\n')

            for (stack in stackTrace) {
                val clazz = stack.className
                if (clazz.contains("org.kingdoms.commands.admin.CommandAdminPurge") && clazz.contains('$')) continue
                if (clazz.equals(KingdomsCommandHandler::class.java.name)) break
                builder.append("     ").append(clazz).append("->").append(stack.methodName).append(':')
                    .append(stack.lineNumber).append('\n')
            }

            return builder.toString()
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerJoin(event: AsyncPlayerPreLoginEvent) {
        if (!PURGING) return
        event.disallow(
            AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
            "${ChatColor.DARK_RED} Server is currently whitelisted by kingdoms"
        )
    }

    @EventHandler
    fun onCommand(event: ServerCommandEvent) {
        if (!PURGING) return

        if (event.command == "stop") {
            if (DONE) Bukkit.shutdown()
            else {
                AdminToolsLang.COMMAND_ADMIN_PURGE_STOP.sendError(event.sender)
            }
        } else {
            AdminToolsLang.COMMAND_ADMIN_PURGE_COMMAND.sendError(event.sender)
            event.isCancelled = true
        }
    }

    override fun execute(context: CommandContext): CommandResult {
        if (context.getMessageReceiver() is Player) {
            context.sendError(KingdomsLang.COMMANDS_CONSOLE_ONLY)
            return CommandResult.FAILED
        }

        if (PURGING) {
            context.sendError(AdminToolsLang.COMMAND_ADMIN_PURGE_ALREADY)
            return CommandResult.FAILED
        }

        if (!FULLY_LOADED) {
            context.sendError(AdminToolsLang.COMMAND_ADMIN_PURGE_NOT_LOADED)
            return CommandResult.FAILED
        }

        if (ACCEPT_COOLDOWN == 0L || Duration.ofSeconds(5)
                .minus(Duration.ofMillis(System.currentTimeMillis() - ACCEPT_COOLDOWN)).isNegative
        ) {
            ACCEPT_COOLDOWN = System.currentTimeMillis()
            context.sendError(AdminToolsLang.COMMAND_ADMIN_PURGE_CONFIRM)
            return CommandResult.PARTIAL
        }

        PURGING = true
        ConfigWatcher.setAccepting(false)
        val errors: MutableList<Throwable> = mutableListOf()

        errors.tryThis {
            ChatInputManager.endAllConversations()
            InteractiveGUIManager.getGuis().values.forEach { x -> x.owner.closeInventory() }
            Bukkit.getServer().onlinePlayers.forEach { x -> x.kickPlayer("${ChatColor.DARK_RED}Kingdoms purging process has started.") }
        }

        context.sendMessage(AdminToolsLang.COMMAND_ADMIN_PURGE_PURGING)
        val data = KingdomsDataCenter.get()
        val removeCtx = KingdomItemRemoveContext().apply { dropsItem = false }
        for (land in data.landManager.peekAllData()) {
            try {
                land.structures.values.forEach { x -> errors.tryThis { x.remove(removeCtx) } }
            } catch (ignored: ConcurrentModificationException) {
                KLogger.warn("ConcurrentModificationException for structures")
            }
            try {
                land.turrets.values.forEach { x -> errors.tryThis { x.remove(removeCtx) } }
            } catch (ignored: ConcurrentModificationException) {
                KLogger.warn("ConcurrentModificationException for turrets")
            }
            try {
                land.protectedBlocks.values.forEach { x -> x.sign.block.type = Material.AIR }
            } catch (ignored: ConcurrentModificationException) {
                KLogger.warn("ConcurrentModificationException for protectedBlocks")
            }
        }

        for (kingdom in data.kingdomManager.kingdoms) {
            kingdom.disband(GroupDisband.Reason.ADMIN)
        }
        for (nation in data.nationManager.nations) {
            nation.disband(GroupDisband.Reason.ADMIN)
        }

        errors.tryThis {
            data.kingdomPlayerManager.clearCache()
            data.landManager.clearCache()
            data.kingdomManager.clearCache()
            data.nationManager.clearCache()
            data.mtg.clearCache()
        }

        if (KingdomsConfig.DATABASE_USE_DATA_FOLDER.manager.boolean) {
            errors.tryThis { FSUtil.deleteFolder(KingdomsDataCenter.DATA_FOLDER) }
        } else {
            arrayOf(
                data.kingdomPlayerManager,
                data.landManager,
                data.kingdomManager,
                data.nationManager,
                data.mtg
            ).forEach { x ->
                errors.tryThis { x.deleteAllData() }
            }
        }

        if (errors.isEmpty()) {
            context.sendMessage(AdminToolsLang.COMMAND_ADMIN_PURGE_DONE)
        } else {
            context.sendMessage(AdminToolsLang.COMMAND_ADMIN_PURGE_DONE_WITH_ERRORS)
            // toSet() to remove duplicates
            val error = errors.map { x -> sanitizeStackTrace(x) }
                .toSet().joinToString("---------------------------------------------------------------\n")
            KLogger.error(error)
        }

        DONE = true

        KLogger.info("Purging is done. Stopping the server in 5 seconds...")
        Kingdoms.taskScheduler().async().delayed(Duration.ofSeconds(5), { ->
            KLogger.info("Shutting down...")
            Bukkit.shutdown()
        })
        return CommandResult.SUCCESS
    }
}