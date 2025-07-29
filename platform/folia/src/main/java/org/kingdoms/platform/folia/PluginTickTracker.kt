package org.kingdoms.platform.folia

import org.bukkit.plugin.Plugin
import org.kingdoms.server.core.ServerTickTracker

interface PluginTickTracker : ServerTickTracker {
    fun start(plugin: Plugin)
}
