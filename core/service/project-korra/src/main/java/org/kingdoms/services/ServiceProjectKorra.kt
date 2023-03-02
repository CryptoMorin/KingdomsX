package org.kingdoms.services

import com.projectkorra.projectkorra.ability.CoreAbility
import com.projectkorra.projectkorra.config.ConfigManager
import com.projectkorra.projectkorra.region.RegionProtection
import org.bukkit.Location
import org.bukkit.entity.Player

class ServiceProjectKorra(val handler: (player: Player, location: Location) -> Boolean) : Service {
    override fun enable() {
        if (protection == null) protection = KingdomsRegionProtection()
    }

    fun registerConfig() {
        // https://github.com/ProjectKorra/ProjectKorra/blob/master/src/main/java/com/projectkorra/projectkorra/config/ConfigManager.java
        val config = ConfigManager.defaultConfig.get()
        config.addDefault("Properties.RegionProtection.Kingdoms.Respect", true)
        config.addDefault("Properties.RegionProtection.Kingdoms.ProtectDuringInvasions", false)
    }

    companion object {
        private var protection: KingdomsRegionProtection? = null

        // https://github.com/ProjectKorra/ProjectKorra/blob/master/src/main/java/com/projectkorra/projectkorra/GeneralMethods.java#L1551
        @JvmStatic
        fun isEnabled(): Boolean =
            ConfigManager.defaultConfig.get().getBoolean("Properties.RegionProtection.Kingdoms.Respect")

        @JvmStatic
        fun protectDuringInvasions(): Boolean =
            ConfigManager.defaultConfig.get().getBoolean("Properties.RegionProtection.Kingdoms.ProtectDuringInvasions")
    }

    inner class KingdomsRegionProtection : RegionProtection("Kingdoms", "Kingdoms.Respect") {
        override fun isRegionProtectedReal(
            player: Player, location: Location, ability: CoreAbility?,
            harmless: Boolean, igniteAbility: Boolean, explosiveAbility: Boolean
        ): Boolean {
            // https://github.com/ProjectKorra/ProjectKorra/blob/master/src/main/java/com/projectkorra/projectkorra/region/ProtectionManager.java
            return handler(player, location)
        }
    }
}
