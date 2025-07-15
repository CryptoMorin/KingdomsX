package org.kingdoms.commands.admin.sql

import org.bukkit.event.Listener
import org.bukkit.permissions.PermissionDefault
import org.kingdoms.commands.KingdomsParentCommand
import org.kingdoms.commands.admin.CommandAdmin
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm

@Cmd("sql")
@CmdParent(CommandAdmin::class)
@CmdPerm(PermissionDefault.OP)
class CommandAdminSQL : KingdomsParentCommand(), Listener {
    init {
        if (!isDisabled) {
            CommandAdminSQLStatement()
        }
    }
}