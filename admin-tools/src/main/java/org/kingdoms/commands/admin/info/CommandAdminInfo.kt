package org.kingdoms.commands.admin.info

import org.bukkit.permissions.PermissionDefault
import org.kingdoms.commands.KingdomsParentCommand
import org.kingdoms.commands.admin.CommandAdmin
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm
import org.kingdoms.server.permission.PermissionDefaultValue

@Cmd("info")
@CmdParent(CommandAdmin::class)
@CmdPerm(PermissionDefaultValue.OP)
class CommandAdminInfo : KingdomsParentCommand() {
    init {
        CommandAdminInfoPlayer(this)
        CommandAdminInfoKingdom(this)
    }
}