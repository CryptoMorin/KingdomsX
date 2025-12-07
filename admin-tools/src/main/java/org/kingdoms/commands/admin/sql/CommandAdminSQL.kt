package org.kingdoms.commands.admin.sql

import org.bukkit.event.Listener
import org.bukkit.permissions.PermissionDefault
import org.kingdoms.commands.KingdomsParentCommand
import org.kingdoms.commands.admin.CommandAdmin
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm
import org.kingdoms.server.permission.PermissionDefaultValue

@Cmd("sql")
@CmdParent(CommandAdmin::class)
@CmdPerm(PermissionDefaultValue.OP)
class CommandAdminSQL : KingdomsParentCommand() {
    init {
        if (!isDisabled) {
            CommandAdminSQLPassword()
            CommandAdminSQLStatement()
        }
    }
}