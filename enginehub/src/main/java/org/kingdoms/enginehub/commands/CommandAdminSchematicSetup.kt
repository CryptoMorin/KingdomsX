package org.kingdoms.enginehub.commands

import org.kingdoms.commands.CommandContext
import org.kingdoms.commands.CommandResult
import org.kingdoms.commands.KingdomsCommand
import org.kingdoms.commands.KingdomsParentCommand
import org.kingdoms.constants.land.abstraction.KingdomBuilding
import org.kingdoms.constants.land.structures.StructureRegistry
import org.kingdoms.enginehub.EngineHubAddon
import org.kingdoms.enginehub.EngineHubLang
import org.kingdoms.utils.config.adapters.YamlContainer
import org.kingdoms.utils.config.importer.YamlModuleLoader
import org.snakeyaml.nodes.AliasNode

class CommandAdminSchematicSetup(parent: KingdomsParentCommand) : KingdomsCommand("setup", parent) {
    override fun execute(context: CommandContext): CommandResult {
        setup()
        context.sendMessage(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_SETUP_DONE)
        return CommandResult.SUCCESS
    }

    companion object {
        @JvmStatic
        fun setup() {
            val buildingAdapter = YamlModuleLoader.get("building")!!.adapter
            setupBuilding(buildingAdapter)

            // Nexus has 2 more levels by default
            val nexusStyle = StructureRegistry.get().getStyle("nexus")
            if (nexusStyle !== null) {
                setupBuilding(nexusStyle.config)
            } else {
                EngineHubAddon.INSTANCE.logger.info("'nexus' style was not found, skipping setup for this structure...")
            }

            buildingAdapter.saveConfig()

            buildingAdapter.reloadHandle()
            for (land in plugin.dataCenter.landManager.loadedData) {
                for (block in land.kingdomBlocks) {
                    if (block is KingdomBuilding<*>) {
                        block.update()
                    }
                }
            }
        }

        private fun setupBuilding(buildingAdapter: YamlContainer) {
            val buildingYml = buildingAdapter.config
            val buildingSec = buildingYml.getSection("building")

            for (key in buildingSec.keys.toTypedArray()) { // For each level
                val levelSection = buildingSec.getSection(key)

                run { // Set schematic paths
                    levelSection.set("schematic", "<type>/<name>/$key")
                }

                run { // Change functional points
                    val fnPoints = levelSection.getSection("functional-points")
                    val originKey = fnPoints.keys.find { it.replace(" ", "") == "0,0,0" }
                    if (originKey != null) {
                        val origin = fnPoints.getNode(originKey)!!
                        fnPoints.node.pairs.clear()
                        fnPoints.node.put("0, 1, 0", origin)
                    }
                }

                run { // Increase holograms height
                    val holograms = levelSection.getSection("holograms")
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
        }
    }
}