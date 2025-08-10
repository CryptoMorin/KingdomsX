package org.kingdoms.commands.admin.info

import org.bukkit.OfflinePlayer
import org.kingdoms.admintools.AdminToolsLang
import org.kingdoms.commands.*
import org.kingdoms.commands.general.others.CommandShow
import org.kingdoms.locale.KingdomsLang
import org.kingdoms.locale.placeholders.target.PlaceholderTarget
import org.kingdoms.utils.KingdomsBukkitExtensions.asOfflinePlayer

class CommandAdminInfoKingdom(parent: KingdomsParentCommand) : KingdomsCommand("kingdom", parent) {
    override fun execute(context: CommandContext): CommandResult {
        context.requireArgs(1)
        val kingdom = context.getKingdom(0) ?: return CommandResult.FAILED

        context.messageContext.withContext(kingdom).apply {
            raw("id", kingdom.id)
            raw("ranks", kingdom.ranks.toString())
        }

        val placeholder: OfflinePlayer = kingdom.king?.offlinePlayer ?: kingdom.members.first()?.asOfflinePlayer()!!
        CommandShow.show(context.getMessageReceiver(), kingdom, PlaceholderTarget.of(placeholder), true, false)
        context.sendMessage(AdminToolsLang.COMMAND_ADMIN_INFO_KINGDOM_MESSAGE)
        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): MutableList<String> {
        return context.getKingdoms(0)
    }
}