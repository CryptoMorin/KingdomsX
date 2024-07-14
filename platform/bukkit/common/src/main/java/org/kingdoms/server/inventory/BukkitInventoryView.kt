package org.kingdoms.server.inventory

import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

interface BukkitInventoryView {
    fun getTopInventory(): Inventory
    fun getBottomInventory(): Inventory
    fun getPlayer(): HumanEntity
    fun getType(): InventoryType
    fun setItem(var1: Int, var2: ItemStack?)
    fun getItem(var1: Int): ItemStack?
    fun setCursor(var1: ItemStack?)
    fun getCursor(): ItemStack?
    fun convertSlot(var1: Int): Int
    fun close()
    fun countSlots(): Int
    fun getTitle(): String
}