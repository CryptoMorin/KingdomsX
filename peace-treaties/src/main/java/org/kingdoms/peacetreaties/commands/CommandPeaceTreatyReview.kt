package org.kingdoms.peacetreaties.commands

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.kingdoms.commands.*
import org.kingdoms.constants.group.Kingdom
import org.kingdoms.gui.GUIAccessor
import org.kingdoms.gui.InteractiveGUI
import org.kingdoms.locale.ContextualMessageSender
import org.kingdoms.peacetreaties.config.PeaceTreatyGUI
import org.kingdoms.peacetreaties.config.PeaceTreatyLang
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getProposedPeaceTreaties
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getReceivedPeaceTreaties
import org.kingdoms.peacetreaties.data.PeaceTreaty

class CommandPeaceTreatyReview(parent: KingdomsParentCommand) : KingdomsCommand("review", parent) {
    companion object {
        @JvmStatic
        fun openGUI(player: Player, kingdom: Kingdom): InteractiveGUI {
            val gui = GUIAccessor.prepare(player, PeaceTreatyGUI.`PEACE$TREATIES`)
            gui.messageContext.raw("page", 1).raw("pages", 1)

            val sentOpt = gui.getReusableOption("sent")!!
            val receivedOpt = gui.getReusableOption("received")!!

            for (contract in kingdom.getReceivedPeaceTreaties().values) {
                contract.getPlaceholderContextProvider(receivedOpt.messageContext)
                receivedOpt.onNormalClicks { ctx ->
                    showDetails(ctx, contract, !contract.isAccepted)
                    player.closeInventory()
                }
                receivedOpt.pushHead(Bukkit.getOfflinePlayer(contract.requesterPlayerID), false)
            }

            for (contract in kingdom.getProposedPeaceTreaties().values) {
                contract.getPlaceholderContextProvider(sentOpt.messageContext)
                sentOpt.on(ClickType.LEFT) { ctx ->
                    showDetails(ctx, contract, false)
                    player.closeInventory()
                }
                sentOpt.on(ClickType.RIGHT) { _ ->
                    contract.revoke()
                    openGUI(player, kingdom)
                }
                sentOpt.pushHead(Bukkit.getOfflinePlayer(contract.requesterPlayerID), false)
            }

            gui.open()
            return gui
        }

        fun showDetails(context: ContextualMessageSender, contract: PeaceTreaty, showAcceptTip: Boolean) {
            contract.getPlaceholderContextProvider(context.messageContext)
            context.sendMessage(PeaceTreatyLang.COMMAND_PEACETREATY_REVIEW_HEADER)

            for (termGrouping in contract.terms.values) {
                for (term in termGrouping.terms.values) {
                    context.messageContext.raw("term_message", term.provider.message)
                    term.addEdits(context.messageContext)
                    context.sendMessage(PeaceTreatyLang.COMMAND_PEACETREATY_REVIEW_TERMS)
                }
            }

            if (showAcceptTip) context.sendMessage(PeaceTreatyLang.COMMAND_PEACETREATY_REVIEW_FOOTER)
        }
    }

    override fun execute(context: CommandContext): CommandResult {
        if (context.assertPlayer()) return CommandResult.FAILED
        if (context.assertHasKingdom()) return CommandResult.FAILED
        val kingdom = context.kingdom

        if (!context.assertArgs(1)) {
            openGUI(context.senderAsPlayer(), kingdom)
            return CommandResult.SUCCESS
        }

        val targetKingdom = context.getKingdom(0) ?: return CommandResult.FAILED
        context.messageContext.withContext(targetKingdom)

        val contract: PeaceTreaty = kingdom.getReceivedPeaceTreaties()[targetKingdom.key] ?: kotlin.run {
            context.sendError(PeaceTreatyLang.COMMAND_PEACETREATY_NO_CONTRACT_FROM_KINGDOM)
            return CommandResult.FAILED
        }

        showDetails(context, contract, !contract.isAccepted)

        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): List<String> {
        return CommandPeaceTreaty.tabCompleteKingdomsWithContract(context)
    }
}