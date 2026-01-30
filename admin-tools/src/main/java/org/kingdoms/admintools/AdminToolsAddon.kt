package org.kingdoms.admintools

import org.bukkit.plugin.java.JavaPlugin
import org.kingdoms.addons.Addon
import org.kingdoms.admintools.claimschematic.ClaimSchematicRegistry
import org.kingdoms.commands.admin.*
import org.kingdoms.commands.admin.debugging.CommandAdminDebugHitPoints
import org.kingdoms.commands.admin.debugging.CommandAdminSound
import org.kingdoms.commands.admin.item.CommandAdminItemEditor
import org.kingdoms.commands.admin.sql.CommandAdminSQL
import org.kingdoms.gui.GUIConfig
import org.kingdoms.locale.LanguageManager
import org.kingdoms.main.Kingdoms
import java.io.File

class AdminToolsAddon : JavaPlugin(), Addon {
    companion object {
        @JvmStatic lateinit var INSTANCE: AdminToolsAddon
    }

    val claimSchematics = ClaimSchematicRegistry(Kingdoms.getPath("claim-schematics"))

    init {
        INSTANCE = this
    }

    override fun onLoad() {
        LanguageManager.registerMessenger(AdminToolsLang::class.java)
    }

    override fun onEnable() {
        if (!isKingdomsEnabled) return

        GUIConfig.loadInternalGUIs(this);
        reloadAddon()
        registerAddon()

        claimSchematics
            .useDefaults(false)
            .copyDefaults(false)
            .register()
    }

    override fun reloadAddon() {
        if (!isKingdomsEnabled) return
        CommandAdminClaimSchematic()
        CommandAdminDebugHitPoints()
        CommandAdminSQL()
        CommandAdminItemEditor()
        CommandAdminResetKingdom()
        CommandAdminExecuteBook()
        CommandAdminCondition()
        CommandAdminCommands()
        CommandAdminSound()
        CommandAdminForeach()
        CommandAdminFiles()
        CommandAdminMissingGUIs()
        CommandAdminEntity()
        CommandAdminGUI()
        CommandAdminSearchConfig()
        CommandAdminResetConfigs()
        CommandAdminTurret()
        CommandAdminPurge()
    }

    override fun onDisable() {
        super.onDisable()
        signalDisable()
    }

    override fun getFile(): File = super.getFile()
    override fun getAddonName(): String = "admin-tools"
}