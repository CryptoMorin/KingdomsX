package org.kingdoms.enginehub.commands

import org.kingdoms.commands.CommandContext
import org.kingdoms.commands.CommandResult
import org.kingdoms.commands.KingdomsCommand
import org.kingdoms.commands.KingdomsParentCommand
import org.kingdoms.enginehub.EngineHubLang
import org.kingdoms.utils.config.importer.YamlModuleLoader
import org.snakeyaml.nodes.AliasNode

class CommandAdminSchematicSetup(parent: KingdomsParentCommand) : KingdomsCommand("setup", parent) {
    override fun execute(context: CommandContext): CommandResult {
        val buildingAdapter = YamlModuleLoader.get("building")!!.adapter
        val buildingYml = buildingAdapter.config
        val buildingSec = buildingYml.getSection("building")

        for (key in buildingSec.keys) {
            run { // Change functional points
                val fnPoints = buildingSec.getSection(key, "functional-points")
                // KLogger.temp("has fn points for $key -> $buildingSec -> $fnPoints")
                val originKey = fnPoints.keys.find { it.replace(" ", "") == "0,0,0" }
                if (originKey != null) {
                    val origin = fnPoints.getNode(originKey)!!
                    fnPoints.node.pairs.clear()
                    fnPoints.node.put("0, 1, 0", origin)
                }
            }
            run {
                val holograms = buildingSec.getSection(key, "holograms")
                for (hologramType in holograms.keys) {
                    val hologramTypes = holograms.getSection(hologramType)
                    for (hologramGroup in hologramTypes.keys) {
                        val hologram = hologramTypes.getSection(hologramGroup)
                        if (hologram.getNode("height") is AliasNode) continue
                        hologram.set("height", 2.5)
                    }
                }
            }
        }

        buildingAdapter.saveConfig()
        context.sendMessage(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_SETUP_DONE)
        return CommandResult.SUCCESS
    }
}