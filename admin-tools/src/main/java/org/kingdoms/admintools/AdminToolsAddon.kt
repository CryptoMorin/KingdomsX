package org.kingdoms.admintools

import org.bukkit.plugin.java.JavaPlugin
import org.kingdoms.addons.Addon
import org.kingdoms.commands.admin.debugging.CommandAdminDebugHitPoints
import org.kingdoms.commands.admin.sql.CommandAdminSQL
import org.kingdoms.locale.LanguageManager
import java.io.File

class AdminToolsAddon : JavaPlugin(), Addon {
    companion object {
        @JvmStatic lateinit var INSTANCE: AdminToolsAddon
    }

    init {
        INSTANCE = this
    }

    override fun onLoad() {
        LanguageManager.registerMessenger(AdminToolsLang::class.java)
    }

    override fun onEnable() {
        if (!isKingdomsEnabled) return
        reloadAddon()
        registerAddon()
    }

    override fun reloadAddon() {
        if (!isKingdomsEnabled) return
        CommandAdminDebugHitPoints()
        CommandAdminSQL()
    }

    override fun onDisable() {
        super.onDisable()
        signalDisable()
    }

    override fun getFile(): File = super.getFile()
    override fun getAddonName(): String = "admin-tools"
}