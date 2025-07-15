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
import org.kingdoms.utils.config.adapters.YamlParseContext
import org.kingdoms.utils.config.adapters.YamlResource
import org.kingdoms.utils.config.importer.YamlModuleLoader
import org.snakeyaml.nodes.AliasNode
import org.snakeyaml.nodes.MappingNode
import org.snakeyaml.nodes.SequenceNode
import java.util.function.Consumer

class CommandAdminSchematicSetup(parent: KingdomsParentCommand) : KingdomsCommand("setup", parent) {
    override fun execute(context: CommandContext): CommandResult {
        setup()
        context.sendMessage(EngineHubLang.COMMAND_ADMIN_SCHEMATIC_SETUP_DONE)
        return CommandResult.SUCCESS
    }

    companion object {
        @JvmStatic
        fun setup() {
            modifyDeclaration("building") { setupBuilding(it, null, null) }
            modifyDeclaration("turret") { setupBuilding(it, "0, 1, 0", null) }

            // Nexus has 2 more levels by default
            modifyStructure("nexus") {
                // (import):
                //   building:
                //     anchors: [ &fnPoints fnPoints, &holograms holograms ]
                //                ^^^^^^^^^^^^^^^^^^
                val importedAnchors = it.config.findNode(arrayOf("(import)", "building", "anchors")) as SequenceNode
                importedAnchors.value.removeAt(0)
                (importedAnchors.parsed as? MutableList<*>)?.removeAt(0)

                setupBuilding(it, "0, 1, 0", null)
            }

            modifyStructure("powercell") { setupBuilding(it, "0, 1, 0", null) }
            modifyStructure("regulator") { setupBuilding(it, "0, 1, 0", null) }
            modifyStructure("outpost") { setupBuilding(it, "0, 1, 0", 4.0) }
            modifyStructure("warppad") { setupBuilding(it, "0, 2, 0", 5.0) }
            modifyStructure("siege-cannon") { setupBuilding(it, "0, 2, 0", 5.0) }

            updateKingdomBuildings()
        }

        private fun updateKingdomBuildings() {
            for (land in plugin.dataCenter.landManager.loadedData) {
                for (block in land.kingdomBlocks) {
                    if (block is KingdomBuilding<*>) {
                        block.update()
                    }
                }
            }
        }

        fun modifyDeclaration(declName: String, operation: Consumer<YamlContainer>) {
            val decl = YamlModuleLoader.get(declName)
            modifyConfig(decl?.adapter, "'$decl' declaration", operation)
        }

        fun modifyStructure(styleName: String, operation: Consumer<YamlContainer>) {
            val style = StructureRegistry.get().getStyle(styleName)
            modifyConfig(style?.config, "'$style' structure style", operation)
        }

        fun modifyConfig(adapter: YamlContainer?, errorDisplay: String, operation: Consumer<YamlContainer>) {
            if (adapter !== null) {
                val yaml = YamlResource(adapter.file!!).setResolveAliases(false).load()

                operation.accept(yaml)
                yaml.saveConfig()
                adapter.load()
                adapter.reloadHandle()
            } else {
                EngineHubAddon.INSTANCE.logger.info("$errorDisplay was not found, skipping setup for it...")
            }
        }

        private fun setupBuilding(buildingAdapter: YamlContainer, shiftFnPoints: String?, hologramsHeight: Double?) {
            val buildingYml = buildingAdapter.config
            val buildingSec = buildingYml.createSection("building")

            @Suppress("YAMLUnresolvedAlias")
            val fnPointOneAlias = YamlContainer.parse(
                YamlParseContext()
                    .named("EngineHub's Building Config Setup Injector")
                    .shouldResolveAliases(false)
                    .stream(
                        """
                        # [Final]
                        functional-points: *fnPoints
                      """.trimMargin()
                    )
            )

            @Suppress("YAMLUnresolvedAlias")
            val hologramsHeightNodes: MappingNode? = if (hologramsHeight === null) null else YamlContainer.parse(
                YamlParseContext()
                    .named("EngineHub's Building Config Setup Injector")
                    .shouldResolveAliases(false)
                    .stream(
                        """
                        holograms:
                          main:
                            main:
                              height: *hologram-height
                          opening:
                            main:
                              height: *hologram-height
                          upgrading:
                            main:
                              height: *hologram-height
                          demolishing:
                            main:
                              height: *hologram-height
                          repairing:
                            main:
                              height: *hologram-height
                      """.trimMargin()
                    )
            )

            @Suppress("YAMLUnusedAnchor")
            val fnPointOneAnchor =
                if (shiftFnPoints === null || shiftFnPoints.isEmpty()) null else YamlContainer.parse(
                    YamlParseContext()
                        .named("EngineHub's Building Config Setup Injector")
                        .stream(
                            """
                            # [Final]
                            functional-points: &fnPoints
                              $shiftFnPoints:
                                - {type: activation, name: main}
                                - {type: interaction, name: main}
                                - {type: manual, name: main}
                        """.trimMargin()
                        )
                )

            for (key in buildingSec.keys.toTypedArray()) { // For each level
                val levelSection =
                    buildingSec.getSection(key) ?: error("Unexpected building $key -> ${buildingSec.getNode(key)}")

                run { // Set schematic paths
                    levelSection.addNode("schematic: '<type>/<name>/$key'")
                }

                run { // Change functional points
                    if (shiftFnPoints == "") {
                        levelSection.remove("functional-points")
                        levelSection.node.merge(fnPointOneAlias)
                    }
                }

                run { // Increase holograms height for building.yml
                    val holograms = levelSection.getSection("holograms")
                    if (holograms !== null) {
                        for (hologramType in holograms.keys) {
                            val hologramTypes = holograms.getSection(hologramType)
                            for (hologramGroup in hologramTypes.keys) {
                                val hologram = hologramTypes.getSection(hologramGroup)
                                if (hologram.getNode("height") is AliasNode) continue

                                @Suppress("YAMLUnusedAnchor")
                                hologram.addNode("height: &height 2.5")
                            }
                        }
                    }
                }
            }

            if (fnPointOneAnchor !== null || hologramsHeightNodes !== null) {
                if (hologramsHeightNodes !== null) {
                    @Suppress("YAMLUnusedAnchor")
                    buildingYml.addNode("'[holograms-height]': &hologram-height $hologramsHeight")
                }

                // We set these later to avoid the loop of pre-existing values.
                for (key in arrayOf("1", "2", "3")) {
                    val levelSection = buildingSec.createSection(key)

                    if (fnPointOneAnchor !== null) {
                        if (key == "1") levelSection.node.merge(fnPointOneAnchor)
                        else levelSection.node.merge(fnPointOneAlias)
                    }

                    if (hologramsHeightNodes !== null) {
                        levelSection.node.merge(hologramsHeightNodes)
                    }
                }
            }
        }
    }
}