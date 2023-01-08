package org.kingdoms.peacetreaties.commands

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.kingdoms.commands.*
import org.kingdoms.constants.group.Kingdom
import org.kingdoms.gui.GUIAccessor
import org.kingdoms.gui.InteractiveGUI
import org.kingdoms.locale.ContextualMessenger
import org.kingdoms.locale.KingdomsLang
import org.kingdoms.peacetreaties.config.PeaceTreatyGUI
import org.kingdoms.peacetreaties.config.PeaceTreatyLang
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getProposedPeaceTreaties
import org.kingdoms.peacetreaties.data.PeaceTreaties.Companion.getReceivedPeaceTreaties
import org.kingdoms.peacetreaties.data.PeaceTreaty
import org.kingdoms.peacetreaties.terms.types.KeepLandsTerm
import org.kingdoms.utils.LocationUtils
import java.util.stream.Collectors

class CommandPeaceTreatyReview(parent: KingdomsParentCommand) : KingdomsCommand("review", parent) {
    companion object {
        @JvmStatic
        fun openGUI(player: Player, kingdom: Kingdom): InteractiveGUI {
            val gui = GUIAccessor.prepare(player, PeaceTreatyGUI.`PEACE$TREATIES`)
            gui.edits.raw("page", 1).raw("pages", 1)

            val sentOpt = gui.getReusableOption("sent")
            val receivedOpt = gui.getReusableOption("received")

            for (contract in kingdom.getReceivedPeaceTreaties().values) {
                contract.getPlaceholderContextProvider(receivedOpt.settings)
                receivedOpt.onNormalClicks { ctx ->
                    showDetails(ctx, contract)
                    player.closeInventory()
                }
                receivedOpt.pushHead(Bukkit.getOfflinePlayer(contract.requesterPlayerID))
            }

            for (contract in kingdom.getProposedPeaceTreaties().values) {
                contract.getPlaceholderContextProvider(sentOpt.settings)
                sentOpt.onNormalClicks { ctx ->
                    showDetails(ctx, contract)
                    player.closeInventory()
                }
                sentOpt.pushHead(Bukkit.getOfflinePlayer(contract.requesterPlayerID))
            }

            gui.openInventory()
            return gui
        }

        fun showDetails(context: ContextualMessenger, contract: PeaceTreaty) {
            contract.getPlaceholderContextProvider(context.settings)
            context.sendMessage(PeaceTreatyLang.COMMAND_PEACETREATY_REVIEW_HEADER)

            for (termGrouping in contract.terms.values) {
                for (term in termGrouping.terms.values) {
                    context.settings.raw("term_message", term.provider.message)
                    term.addEdits(context.settings)

                    if (term is KeepLandsTerm) {
                        val returningLands = KeepLandsTerm.getInvadedLands(contract.proposerKingdom, contract.victimKingdomId)
                        returningLands.removeAll(term.keptLands)

                        context.settings.parse(
                            "term_keep_lands_returning_lands", "{\$s}" + returningLands.stream()
                                .map { x -> KingdomsLang.LOCATIONS_CHUNK.parse(*LocationUtils.getChunkEdits(x)) }
                                .collect(Collectors.joining("{\$sep}| {\$s}"))
                        )
                    }
                    context.sendMessage(PeaceTreatyLang.COMMAND_PEACETREATY_REVIEW_TERMS)
                }
            }

            context.sendMessage(PeaceTreatyLang.COMMAND_PEACETREATY_REVIEW_FOOTER)
        }
    }

    override fun executeX(context: CommandContext): CommandResult {
        if (context.assertPlayer()) return CommandResult.FAILED
        if (context.assertHasKingdom()) return CommandResult.FAILED
        val kingdom = context.kingdom

        if (!context.assertArgs(1)) {
            openGUI(context.senderAsPlayer(), kingdom)
            return CommandResult.SUCCESS
        }

        val targetKingdom = context.getKingdom(0) ?: return CommandResult.FAILED
        context.settings.withContext(targetKingdom)

        val contract: PeaceTreaty = kingdom.getReceivedPeaceTreaties()[targetKingdom.id] ?: kotlin.run {
            context.sendError(PeaceTreatyLang.COMMAND_PEACETREATY_NO_CONTRACT_FROM_KINGDOM)
            return CommandResult.FAILED
        }

        showDetails(context, contract)

        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): List<String> {
        return CommandPeaceTreaty.tabCompleteKingdomsWithContract(context)
    }
}