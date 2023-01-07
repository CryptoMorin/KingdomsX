package org.kingdoms.services

import com.projectkorra.projectkorra.ability.CoreAbility
import com.projectkorra.projectkorra.configuration.ConfigManager
import com.projectkorra.projectkorra.region.RegionProtectionBase
import org.bukkit.Location
import org.bukkit.entity.Player

class ServiceProjectKorra(val handler: (player: Player, location: Location) -> Boolean) : Service {
    override fun enable() {
        if (protection == null) protection = KingdomsRegionProtector()
    }

    fun registerConfig() {
        // https://github.com/ProjectKorra/ProjectKorra/blob/97c28fab4a8e1007ba0deabbe2afc9886efefce6/src/com/projectkorra/projectkorra/configuration/ConfigManager.java
        val config = ConfigManager.defaultConfig.get()
        config.addDefault("Properties.RegionProtection.Kingdoms.Respect", true)
        config.addDefault("Properties.RegionProtection.Kingdoms.ProtectDuringInvasions", false)
    }

    companion object {
        private var protection: KingdomsRegionProtector? = null

        // https://github.com/ProjectKorra/ProjectKorra/blob/e2013bc700b52f72f2963fd2d6775a6923679616/src/com/projectkorra/projectkorra/GeneralMethods.java#L1551
        @JvmStatic
        fun isEnabled(): Boolean =
            ConfigManager.defaultConfig.get().getBoolean("Properties.RegionProtection.Kingdoms.Respect")

        @JvmStatic
        fun protectDuringInvasions(): Boolean =
            ConfigManager.getConfig().getBoolean("Properties.RegionProtection.Kingdoms.ProtectDuringInvasions")
    }

    inner class KingdomsRegionProtector : RegionProtectionBase("Kingdoms", "Kingdoms.Respect") {
        override fun isRegionProtectedReal(
            player: Player, location: Location, ability: CoreAbility?,
            harmless: Boolean, igniteAbility: Boolean, explosiveAbility: Boolean
        ): Boolean {
            // https://github.com/ProjectKorra/ProjectKorra/blob/3ee5c834f96d860be1792b38b2ded2d140dd62bf/src/com/projectkorra/projectkorra/region/Kingdoms.java
            return handler(player, location)
        }
    }
}