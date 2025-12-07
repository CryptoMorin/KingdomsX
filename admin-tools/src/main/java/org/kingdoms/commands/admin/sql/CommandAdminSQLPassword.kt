package org.kingdoms.commands.admin.sql

import org.kingdoms.admintools.AdminToolsLang
import org.kingdoms.commands.CommandContext
import org.kingdoms.commands.CommandResult
import org.kingdoms.commands.CommandTabContext
import org.kingdoms.commands.KingdomsCommand
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm
import org.kingdoms.data.database.sql.DatabaseProperties
import org.kingdoms.server.permission.PermissionDefaultValue
import org.kingdoms.utils.cache.caffeine.ExpirableSet
import org.kingdoms.utils.cache.caffeine.ExpirationStrategy
import org.kingdoms.utils.internal.functional.SecondarySupplier
import java.time.Duration
import java.util.UUID

@Cmd("password")
@CmdParent(CommandAdminSQL::class)
@CmdPerm(PermissionDefaultValue.OP)
class CommandAdminSQLPassword : KingdomsCommand() {
    companion object {
        private val cachedPassword = ExpirableSet<UUID>(ExpirationStrategy.expireAfterCreate(Duration.ofMinutes(5)))

        @JvmStatic
        fun requiresPassword(context: CommandContext): Boolean {
            val password = DatabaseProperties.getFinalPassword()
            if (password.isEmpty()) return false

            if (!cachedPassword.contains(context.receiverUUID)) {
                context.fail(AdminToolsLang.COMMAND_ADMIN_SQL_REQUIRES_PASSWORD)
                return true
            }

            return false
        }
    }

    override fun execute(context: CommandContext): CommandResult {
        context.requireArgs(1)

        val password = DatabaseProperties.getFinalPassword()
        if (password != context.arg(0).substring(1)) {
            return context.fail(AdminToolsLang.COMMAND_ADMIN_SQL_PASSWORD_INVALID_PASSWORD)
        }

        cachedPassword.add(context.receiverUUID)
        context.sendMessage(AdminToolsLang.COMMAND_ADMIN_SQL_PASSWORD_ACCESS_GRANTED)
        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): MutableList<String> {
        return context
            .completeNext(SecondarySupplier { context.tabComplete("<password>") })
            .build()
    }
}