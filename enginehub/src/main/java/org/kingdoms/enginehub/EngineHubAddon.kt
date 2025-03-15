package org.kingdoms.enginehub

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.kingdoms.addons.Addon
import org.kingdoms.commands.admin.CommandAdmin
import org.kingdoms.enginehub.building.WorldEditBuilding
import org.kingdoms.enginehub.building.WorldEditBuildingConstruction
import org.kingdoms.enginehub.commands.CommandAdminSchematic
import org.kingdoms.enginehub.schematic.SchematicManager
import org.kingdoms.enginehub.worldguard.ServiceWorldGuard
import org.kingdoms.enginehub.worldguard.ServiceWorldGuardSeven
import org.kingdoms.enginehub.worldguard.ServiceWorldGuardSix
import org.kingdoms.locale.LanguageManager
import org.kingdoms.main.Kingdoms
import org.kingdoms.services.managers.SoftService
import java.io.File

/**
 * https://enginehub.org/
 */
class EngineHubAddon : JavaPlugin(), Addon {
    companion object {
        @JvmStatic lateinit var INSTANCE: EngineHubAddon
    }

    init {
        INSTANCE = this
    }

    var worldGuard: ServiceWorldGuard? = null

    fun hasWorldGuard(): Boolean = worldGuard != null

    private fun tryLoad(seven: Boolean, errors: MutableList<Throwable>): ServiceWorldGuard? {
        try {
            val wg = if (seven) ServiceWorldGuardSeven()
            else ServiceWorldGuardSix()
            val error = wg.checkAvailability() ?: return wg
            errors.add(error)
        } catch (ex: Throwable) {
            errors.add(ex)
        }
        return null
    }

    override fun onLoad() {
        LanguageManager.registerMessenger(EngineHubLang::class.java)
        EngineHubConfig.register(this)

        initWorldGuard()
        SoftService.WORLD_GUARD.hook(true)
    }

    private fun initWorldGuard() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            val errors: MutableList<Throwable> = mutableListOf()
            var wg = tryLoad(true, errors)
            if (wg == null) wg = tryLoad(false, errors)
            worldGuard = wg

            if (worldGuard == null && Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
                RuntimeException("Failed to load any of the WorldGuard services").apply {
                    errors.forEach { addSuppressed(it) }
                }.printStackTrace()
                return
            }

            if (EngineHubConfig.WORLDGUARD_REGISTER_FLAGS.manager.boolean) {
                var successful = true
                try {
                    logger.info("Registering WorldGuard flags...")
                    worldGuard!!.registerFlags()
                } catch (ex: Throwable) {
                    ex.printStackTrace()
                    successful = false
                }
                if (successful) Kingdoms.get().logger.info("Successfully registered WorldGuard flags.")
            }
        }
    }

    override fun onEnable() {
        if (!isKingdomsEnabled) return

        if (EngineHubConfig.WORLDEDIT_USE_SCHEMATICS.manager.boolean) {
            Kingdoms.get().buildingArchitectRegistry.apply {
                register(WorldEditBuilding.Arch)
                register(WorldEditBuildingConstruction.Arch)
            }
        }

        if (EngineHubConfig.WORLDEDIT_EDIT_PROTECTION.manager.boolean) {
            ServiceWorldEditSessionProtection().enable()
        }

        reloadAddon()

        if (hasWorldGuard()) WorldGuardHandler(this)
        registerAddon()
    }

    override fun reloadAddon() {
        if (!isKingdomsEnabled) return
        Kingdoms.get().buildingArchitectRegistry.apply {
            defaultArchitect = WorldEditBuildingConstruction.Arch
        }

        SchematicManager.loadAll()
        SchematicManager.setup()
        CommandAdminSchematic(CommandAdmin.getInstance())
    }

    override fun onDisable() {
        super.onDisable()
        signalDisable()
    }

    override fun getFile(): File = super.getFile()
    override fun getAddonName(): String = "EngineHub"
}