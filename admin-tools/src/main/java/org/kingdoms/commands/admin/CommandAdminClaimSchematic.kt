package org.kingdoms.commands.admin

import org.bukkit.permissions.PermissionDefault
import org.kingdoms.admintools.AdminToolsAddon
import org.kingdoms.admintools.AdminToolsLang
import org.kingdoms.commands.*
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm
import org.kingdoms.constants.land.location.SimpleChunkLocation
import org.kingdoms.events.lands.ClaimLandEvent
import org.kingdoms.server.location.BlockVector2
import org.kingdoms.server.permission.PermissionDefaultValue
import org.kingdoms.utils.internal.functional.SecondarySupplier

@Cmd("claimschematic")
@CmdParent(CommandAdmin::class)
@CmdPerm(PermissionDefaultValue.OP)
class CommandAdminClaimSchematic : KingdomsParentCommand() {
    init {
        CommandAdminClaimSchematicLoad()
        CommandAdminClaimSchematicSave()
    }
}

@Cmd("load")
@CmdParent(CommandAdminClaimSchematic::class)
class CommandAdminClaimSchematicLoad : KingdomsCommand() {
    override fun execute(context: CommandContext): CommandResult {
        context.assertPlayer()

        val schematicName = context.nextString()
        val schematic = AdminToolsAddon.INSTANCE.claimSchematics.getSchematic(schematicName)
        context.`var`("schematic", schematicName)

        if (schematic === null) {
            return context.fail(AdminToolsLang.COMMAND_ADMIN_CLAIMSCHEMATIC_UNKNOWN_SCHEMATIC)
        }

        val kingdom = context.getKingdom(1) ?: return CommandResult.FAILED
        context.`var`("kingdom", kingdom.name)

        val player = context.senderAsPlayer()
        val center = SimpleChunkLocation.of(player.location)
        val lands = schematic.normalize(center).filter { val land = it.land; land === null || !land.isClaimed }.toSet()

        if (lands.isEmpty()) {
            return context.fail(AdminToolsLang.COMMAND_ADMIN_CLAIMSCHEMATIC_LOAD_NOTHING_CLAIMED)
        }

        if (lands.size != schematic.layout.size) {
            context.sendMessage(AdminToolsLang.COMMAND_ADMIN_CLAIMSCHEMATIC_LOAD_SKIPPED_CHUNKS_WARNING)
        }

        val event = kingdom.claim(lands, context.kingdomPlayer, ClaimLandEvent.Reason.ADMIN, false)
        if (event.isCancelled) {
            return context.fail(AdminToolsLang.COMMAND_ADMIN_CLAIMSCHEMATIC_LOAD_FAILED)
        } else {
            context.sendMessage(AdminToolsLang.COMMAND_ADMIN_CLAIMSCHEMATIC_LOAD_SUCCESS)
            return CommandResult.SUCCESS
        }
    }

    override fun tabComplete(context: CommandTabContext): List<String?> {
        return context.completeNext(SecondarySupplier { AdminToolsAddon.INSTANCE.claimSchematics.schematics.keys })
            .then(SecondarySupplier { context.getKingdoms(context.lastArgumentPosition()) })
            .build()

    }
}

@Cmd("save")
@CmdParent(CommandAdminClaimSchematic::class)
class CommandAdminClaimSchematicSave : KingdomsCommand() {
    override fun execute(context: CommandContext): CommandResult {
        context.assertPlayer()

        val schematicName = context.nextString()
        context.`var`("schematic", schematicName)

        val kingdom = context.getKingdom(1) ?: return CommandResult.FAILED
        context.`var`("kingdom", kingdom.name)

        val player = context.senderAsPlayer()
        val center = SimpleChunkLocation.of(player.location)

        val relativized = HashSet<BlockVector2>()
        for (land in kingdom.landLocations) {
            val relX = land.x - center.x
            val relZ = land.z - center.z
            val normalized = BlockVector2.of(relX, relZ)
            relativized.add(normalized)
        }

        val schematic = AdminToolsAddon.INSTANCE.claimSchematics.saveSchematic(schematicName, relativized)
        context.`var`("file", schematic.file)

        context.sendMessage(AdminToolsLang.COMMAND_ADMIN_CLAIMSCHEMATIC_SAVE_SUCCESS)
        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): List<String?> {
        return context.completeNext(SecondarySupplier {
            val names = AdminToolsAddon.INSTANCE.claimSchematics.schematics.keys
            arrayListOf("<schematic name>").plus(names)
        })
            .then(SecondarySupplier { context.getKingdoms(context.lastArgumentPosition()) })
            .build()

    }
}