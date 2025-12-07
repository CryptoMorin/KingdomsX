package org.kingdoms.commands.admin.sql

import org.bukkit.event.Listener
import org.bukkit.permissions.PermissionDefault
import org.kingdoms.admintools.AdminToolsLang
import org.kingdoms.commands.CommandContext
import org.kingdoms.commands.CommandResult
import org.kingdoms.commands.CommandTabContext
import org.kingdoms.commands.KingdomsCommand
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm
import org.kingdoms.config.KingdomsConfig
import org.kingdoms.data.database.sql.DatabaseProperties
import org.kingdoms.data.database.sql.DatabaseType
import org.kingdoms.data.database.sql.base.SQLDatabase.Companion.stringifyResultSet
import org.kingdoms.locale.KingdomsLang
import org.kingdoms.locale.messenger.StaticMessenger
import org.kingdoms.main.Kingdoms
import org.kingdoms.server.permission.PermissionDefaultValue
import org.kingdoms.utils.cache.single.CachedObject
import org.kingdoms.utils.cache.single.ExpirableObject
import org.kingdoms.utils.internal.functional.SecondarySupplier
import org.kingdoms.utils.internal.string.StringPadder
import java.sql.ResultSet
import java.sql.Statement
import java.time.Duration

@Cmd("statement")
@CmdParent(CommandAdminSQL::class)
@CmdPerm(PermissionDefaultValue.NO_ONE) // Console only
class CommandAdminSQLStatement : KingdomsCommand() {
    override fun execute(context: CommandContext): CommandResult {
        if (context.isPlayer()) {
            return context.fail(KingdomsLang.COMMANDS_CONSOLE_ONLY)
        }

        val dataCenter = Kingdoms.get().dataCenter
        val databaseType = dataCenter.databaseType

        when (databaseType) {
            DatabaseType.SQLite, DatabaseType.H2,
            DatabaseType.MySQL, DatabaseType.PostgreSQL, DatabaseType.MariaDB -> {
            }

            else -> {
                context.`var`("database", databaseType.name)
                return context.fail(AdminToolsLang.COMMAND_ADMIN_SQL_NOT_USING_SQL)
            }
        }

        context.requireArgs(1)

        if (CommandAdminSQLPassword.requiresPassword(context))
            return CommandResult.FAILED

        val hasQuery = context.hasArgs(2)
        val javaClass = Statement::class.java
        val executor = if (hasQuery) {
            javaClass.getMethod(context.arg(0), String::class.java)
        } else {
            javaClass.getMethod(context.arg(0))
        }
        val query = if (hasQuery) databaseType.handleQuery(context.joinArgs(" ", 1)) else ""

        try {
            dataCenter.sqlConnectionProvider.getConnection().use { connection ->
                connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE).use { stmt ->
                    val executeResult = if (hasQuery) {
                        executor.invoke(stmt, query)
                    } else {
                        executor.invoke(stmt)
                    }

                    if (executeResult is ResultSet) {
                        val stringified = executeResult.use { stringifyResultSet(executeResult, null) }
                        if (stringified.isEmpty()) {
                            context.sendMessage(KingdomsLang.NONE)
                        } else {
                            StringPadder()
                                .pad(stringified)
                                .getPadded()
                                .forEach { context.getMessageReceiver().sendMessage("  - $it") }
                        }
                    } else {
                        context.sendMessage(StaticMessenger("{\$p}Result{\$colon} {\$s}$executeResult"))
                    }
                }
            }
        } catch (ex: Throwable) {
            throw RuntimeException(
                "Error while stringifying data with query: " + query + " with " + dataCenter.sqlConnectionProvider.getMetaString(),
                ex
            )
        }

        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): MutableList<String> {
        return context
            .completeNext(SecondarySupplier {
                (Statement::class as Any).javaClass.methods.map { it.name }
            })
            .then(SecondarySupplier { context.tabComplete("<query>") })
            .build()
    }
}