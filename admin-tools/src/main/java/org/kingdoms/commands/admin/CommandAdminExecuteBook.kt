package org.kingdoms.commands.admin

import com.cryptomorin.xseries.XMaterial
import com.cryptomorin.xseries.reflection.XReflection
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.permissions.PermissionDefault
import org.kingdoms.admintools.AdminToolsLang
import org.kingdoms.commands.CommandContext
import org.kingdoms.commands.CommandResult
import org.kingdoms.commands.KingdomsCommand
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdAssertPlayer
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm
import org.kingdoms.constants.item.KingdomItem
import org.kingdoms.constants.item.KingdomItemData
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.locale.SimpleContextualMessageSender
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider
import org.kingdoms.nbt.tag.NBTTagInt
import org.kingdoms.server.inventory.InventorySlot

@Cmd("executeBook")
@CmdParent(CommandAdmin::class)
@CmdPerm(PermissionDefault.OP)
@CmdAssertPlayer
class CommandAdminExecuteBook : KingdomsCommand(), Listener {
    companion object {
        private val NBT_NAMESPACE = Namespace("AdminTools", "EXECUTE_BOOK")

        // Unfortunately only works for Material.WRITTEN_BOOK, so it's useless unless we make it editable.
        private val SUPPORTS_Player_openBook = XReflection.of(Player::class.java)
            .method("public void openBook(org.bukkit.inventory.ItemStack item)")
            .exists()
    }

    @EventHandler(ignoreCancelled = true)
    fun onBookDone(event: PlayerEditBookEvent) {
        val player = event.player

        if (isExecutableItem(getBookItem(event))) {
            handleBook(event.newBookMeta, SimpleContextualMessageSender(player, MessagePlaceholderProvider()))
        }
    }

    @Suppress("DEPRECATION")
    private fun getBookItem(event: PlayerEditBookEvent): ItemStack {
        val slot = event.slot // Deprecated because of -1 offhand slot
        val player = event.player
        val inventory = player.inventory

        return if (slot == -1) {
            inventory.itemInOffHand
        } else {
            inventory.getItem(slot)!!
        }
    }

    private fun handleBook(bookMeta: BookMeta, context: SimpleContextualMessageSender): Boolean {
        if (bookMeta.pageCount < 1) {
            context.sendError(AdminToolsLang.COMMAND_ADMIN_EXECUTEBOOK_BOOK_EMPTY)
            return false
        }

        val pageLines: List<String> = bookMeta.pages.flatMap { listOf(it) }
        if (pageLines.isEmpty()) {
            context.sendError(AdminToolsLang.COMMAND_ADMIN_EXECUTEBOOK_BOOK_EMPTY)
            return false
        }

        var singleCommand = pageLines.joinToString("")
        if (singleCommand.startsWith('/')) singleCommand = singleCommand.substring(1)

        context.senderAsPlayer().performCommand(singleCommand)
        context.sendMessage(AdminToolsLang.COMMAND_ADMIN_RESETKINGDOM_DONE)
        return true
    }

    override fun execute(context: CommandContext): CommandResult {
        val player = context.senderAsPlayer()
        val inventory = player.inventory
        val item = inventory.itemInHand

        val material = XMaterial.matchXMaterial(item)
        if (material !== XMaterial.WRITABLE_BOOK && material !== XMaterial.WRITTEN_BOOK) {
            InventorySlot.Player.HOTBAR.slots.forEach { slot ->
                val hotbarSlot = inventory.getItem(slot)
                if (hotbarSlot === null || hotbarSlot.type === Material.AIR) {
                    val item = createItem(player, slot)

                    inventory.setItem(slot, item)
                    context.sendMessage(AdminToolsLang.COMMAND_ADMIN_EXECUTEBOOK_GIVEN)
                    return CommandResult.PARTIAL
                }
            }

            return context.fail(AdminToolsLang.COMMAND_ADMIN_EXECUTEBOOK_NOT_A_BOOK)
        }

        val bookMeta = item.itemMeta as BookMeta
        val handled = handleBook(bookMeta, context)
        return if (handled) CommandResult.SUCCESS else CommandResult.FAILED
    }

    private fun isExecutableItem(item: ItemStack): Boolean {
        val kItem = KingdomItem.getKingdomItem(item) ?: return false
        return kItem.data.contains(NBT_NAMESPACE)
    }

    private fun createItem(player: Player, slot: Int): ItemStack {
        val item = XMaterial.WRITABLE_BOOK.parseItem()!!
        val kItem = KingdomItem.createKingdomItem(item)
        val data = KingdomItemData.createEmpty(NBT_NAMESPACE).apply {
            createdBy = player.uniqueId
            version = 1
            data = NBTTagInt.of(slot)
        }
        kItem.addData(data)
        kItem.applyChanges()
        return kItem.item
    }
}