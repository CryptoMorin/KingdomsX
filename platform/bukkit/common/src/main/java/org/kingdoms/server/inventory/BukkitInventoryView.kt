package org.kingdoms.server.inventory

import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

interface BukkitInventoryView {
    val topInventory: Inventory
    val bottomInventory: Inventory
    val player: HumanEntity
    val type: InventoryType
    fun setItem(var1: Int, var2: ItemStack?)
    fun getItem(var1: Int): ItemStack?
    fun setCursor(var1: ItemStack?)
    val cursor: ItemStack?
    fun convertSlot(var1: Int): Int
    fun close()
    fun countSlots(): Int
    val title: String
}