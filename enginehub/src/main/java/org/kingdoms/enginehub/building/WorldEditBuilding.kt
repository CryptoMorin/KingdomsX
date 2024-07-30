package org.kingdoms.enginehub.building

import org.kingdoms.constants.land.abstraction.data.SerializationContext
import org.kingdoms.constants.land.building.*
import org.kingdoms.constants.land.building.base.AbstractBuilding
import org.kingdoms.constants.land.building.info.BuildingSettings
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.data.database.dataprovider.SectionableDataGetter
import org.kingdoms.data.database.dataprovider.SectionableDataSetter
import org.kingdoms.enginehub.schematic.SchematicManager
import org.kingdoms.enginehub.schematic.WorldEditSchematic
import org.kingdoms.server.location.BlockLocation3
import org.kingdoms.server.location.Direction
import java.time.Duration

class WorldEditBuilding(
    override val schematic: WorldEditSchematic,
    origin: BlockLocation3, facing: Direction, settings: BuildingSettings
) :
    AbstractBuilding(origin, facing, settings), IWorldEditBuilding {

    override fun getArchitect(): BuildingArchitect = Arch

    private val _region = SimpleBlockRegion(
        SchematicManager.getPopulatedBlocks(
            schematic,
            origin.toVector(),
            facing,
            WorldEditBuildingConstruction.SortingStrategy.BOTTOM_TO_TOP
        ).second.keys
    )

    override fun getRegion(): Region = _region

    object Arch : BuildingArchitect {
        @JvmField val NAMESPACE = Namespace.kingdoms("WORLD_EDIT_FINISHED")
        override fun getNamespace(): Namespace = NAMESPACE
        override fun deserialize(context: BuildingDeserializationContext<SectionableDataGetter>): Building {
            return context.dataProvider.run {
                val origin =
                    context.origin // BlockVector3.fromString(getString("origin")!!).inWorld(context.origin.getWorld())
                val facing = Direction.fromString(getString("facing")!!)!!

                val schematicName = getString("schematic")
                    ?: throw IllegalStateException("Missing schematic name")
                val schematic: WorldEditSchematic = SchematicManager.getSchematic(schematicName)
                    ?: throw IllegalStateException("Missing schematic file named '$schematicName'")

                WorldEditBuilding(schematic, origin, facing, context.settings)
            }
        }
    }

    override fun serialize(context: SerializationContext<SectionableDataSetter>) {
        super<AbstractBuilding>.serialize(context)
        context.dataProvider.apply {
            setString("schematic", schematic.name)
        }
    }

    override fun demolish(filter: RegionFilter?): BuildingDemolition {
        val demolition = WorldEditBuildingConstruction(
            schematic, getOrigin(),
            BuildingConstructionType.DEMOLISHING, BuildingConstructionState.NONE, filter,
            getFacing(), Duration.ZERO
        )
        demolition.prepare(this.getSettings())
        return demolition
    }
}