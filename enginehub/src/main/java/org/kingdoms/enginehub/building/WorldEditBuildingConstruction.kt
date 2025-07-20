package org.kingdoms.enginehub.building

import com.cryptomorin.xseries.XMaterial
import com.cryptomorin.xseries.XTag
import com.sk89q.worldedit.extent.clipboard.Clipboard
import org.bukkit.Material
import org.bukkit.block.Block
import org.kingdoms.constants.land.abstraction.data.SerializationContext
import org.kingdoms.constants.land.building.*
import org.kingdoms.constants.land.building.base.AbstractBuildingConstruction
import org.kingdoms.constants.land.building.info.BuildingConstructionValidation
import org.kingdoms.constants.land.building.info.BuildingSchematic
import org.kingdoms.constants.land.building.info.BuildingSettings
import org.kingdoms.constants.land.building.info.block.BlockDataBlockInfo
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.data.database.dataprovider.SectionableDataGetter
import org.kingdoms.data.database.dataprovider.SectionableDataSetter
import org.kingdoms.enginehub.EngineHubAddon
import org.kingdoms.enginehub.schematic.CalculatedBlocks
import org.kingdoms.enginehub.schematic.SchematicManager
import org.kingdoms.enginehub.schematic.WorldEditSchematic
import org.kingdoms.enginehub.schematic.blocks.ClipboardBlockCopyIterator
import org.kingdoms.enginehub.schematic.blocks.FunctionalWorldEditExtentBlock
import org.kingdoms.enginehub.schematic.blocks.WorldEditExtentBlock
import org.kingdoms.main.KLogger
import org.kingdoms.main.Kingdoms
import org.kingdoms.platform.bukkit.adapters.BukkitAdapter
import org.kingdoms.platform.bukkit.location.BukkitWorld
import org.kingdoms.scheduler.InstantTaskScheduler
import org.kingdoms.scheduler.ScheduledTask
import org.kingdoms.server.location.BlockLocation3
import org.kingdoms.server.location.BlockVector2
import org.kingdoms.server.location.BlockVector3
import org.kingdoms.server.location.Direction
import org.kingdoms.utils.debugging.DebugNS
import org.kingdoms.utils.internal.iterator.RangedIterator
import org.kingdoms.utils.string.ConfigPrinter
import java.time.Duration

private val BuildingSettings.schematic: String?
    get() = this.settings.getString("schematic")

private val BuildingSettings.checkedSchematic: String
    get() {
        val schem = this.schematic
        if (schem !== null) return schem

        ConfigPrinter.printConfig(this.settings)
        error("No schematic is set for the settings above")
    }

class WorldEditBuildingConstruction(
    override val schematic: WorldEditSchematic,
    origin: BlockLocation3,
    type: BuildingConstructionType,
    state: BuildingConstructionState,
    regionFilter: RegionFilter?,
    facing: Direction,
    timePassed: Duration
) : AbstractBuildingConstruction(origin, type, state, regionFilter, facing, timePassed), IWorldEditBuilding {
    var finishedBuilding = false
    override fun getArchitect(): BuildingArchitect = Arch

    private object Debugger : DebugNS {
        override fun namespace(): String = "ENGINEHUB_WORLDEDIT_BUILDING"
    }

    object Arch : BuildingConstructionArchitect {
        @JvmField val NAMESPACE = Namespace.kingdoms("WORLD_EDIT_CONSTRUCTION")
        override fun newBuilding(info: BuildingSchematic): WorldEditBuildingConstruction {
            val schemName = info.settings.checkedSchematic
            val schematic = SchematicManager.getSchematic(schemName)
                ?: error("Cannot find schematic named '$schemName' while constructing a new building")

            val building = WorldEditBuildingConstruction(
                schematic,
                info.origin,
                info.type, BuildingConstructionState.NONE, null,
                info.facing, Duration.ZERO
            )

            building.prepare(info.settings)
            return building
        }

        override fun isSupported(info: BuildingSchematic): Boolean {
            val schematic = info.settings.schematic
            if (schematic === null) return false
            return SchematicManager.getSchematic(schematic) !== null
        }

        override fun getNamespace(): Namespace = NAMESPACE

        override fun deserialize(context: BuildingDeserializationContext<SectionableDataGetter>): WorldEditBuildingConstruction {
            return context.dataProvider.run {
                val facing = Direction.fromString(getString("facing")!!)!!
                val timePassed = getLong("timePassed")
                val blockIndex = getInt("blockIndex")
                val type = BuildingConstructionType.valueOf(getString("type")!!)
                val state = BuildingConstructionState.valueOf(getString("state")!!)
                val sortingStrategy = SortingStrategy.valueOf(getString("sortingStrategy")!!) // Not used yet

                val schematicName = getString("schematic")
                    ?: throw IllegalStateException("Missing schematic name")
                val schematic = SchematicManager.getSchematic(schematicName)
                    ?: throw IllegalStateException("Unknown schematic name '$schematicName'")

                val ctor = WorldEditBuildingConstruction(
                    schematic = schematic,
                    origin = context.origin,
                    type = type,
                    state = state,
                    facing = facing,
                    regionFilter = null,
                    timePassed = Duration.ofMillis(timePassed),
                )
                ctor.blockIndex = blockIndex
                ctor
            }
        }
    }

    override fun serialize(context: SerializationContext<SectionableDataSetter>) {
        super<AbstractBuildingConstruction>.serialize(context)
        context.dataProvider.apply {
            setString("schematic", schematic.name)
            setString("sortingStrategy", sortingStrategy.name)
            setInt("blockIndex", blockIndex)
        }
    }

    private val world = BukkitWorld.from(origin.world)
    private lateinit var blocks: CalculatedBlocks
    private lateinit var transformedClipboard: Clipboard
    private var blockChangeTask: ScheduledTask? = null
    private lateinit var timeBetweenBlocks: Duration
    private val _region: WorldEditRegion
    var blockIndex: Int = -1

    // TODO For demolition, this will not work properly because if a block has ladders on it, breaking
    //      the block will cause ladders to drop as items. Unless we might be able to fix this by simply
    //      cancelling BlockPhysicsEvent for all the building's blocks?
    private val sortingStrategy = if (this.getType() == BuildingConstructionType.DEMOLISHING)
        SortingStrategy.TOP_TO_BOTTOM else SortingStrategy.BOTTOM_TO_TOP

    init {
        populate()
        this._region = WorldEditRegion(this.getOrigin().toVector(), this.blocks, this.transformedClipboard)
    }

    override fun demolish(filter: RegionFilter?): BuildingDemolition {
        prepareForDemolition()
        stopTask()
        val demolition = WorldEditBuildingConstruction(
            schematic, getOrigin(),
            BuildingConstructionType.DEMOLISHING, BuildingConstructionState.NONE, filter,
            getFacing(), Duration.ZERO
        )
        demolition.prepare(this.getSettings())
        return demolition
    }

    override fun validateBlocks(): Map<BlockVector3, BuildingConstructionValidation> {
        val validated: MutableMap<BlockVector3, BuildingConstructionValidation> = hashMapOf()

        for ((loc, block) in blocks) {
            val blockType =
                XMaterial.matchXMaterial(world.getBlockAt(loc.x, loc.y, loc.z).type)
            val canBeReplaced = XTag.AIR.isTagged(blockType) || XTag.FLUID.isTagged(blockType)
            val blockData = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(block.block)
            val validation = BuildingConstructionValidation(
                canBePlaced = canBeReplaced,
                configEntry = if (canBeReplaced) "ok" else "conflict",
                actualBlock = BlockDataBlockInfo(null, blockData, getFacing())
            )
            validated[loc] = validation
        }

        return validated
    }

    override fun getRegion(): Region = _region

    private fun populate() {
        try {
            val (transformedClipboard, blocks) = SchematicManager.getPopulatedBlocks(
                schematic,
                getOrigin().toVector(),
                getFacing(), sortingStrategy
            )
            this.transformedClipboard = transformedClipboard
            if (regionFilter == null) this.blocks = blocks
            else this.blocks = blocks.filter { regionFilter!!.filter(it.key) }.toMutableMap()
        } catch (ex: Throwable) {
            throw BuildingConstructionException(
                this,
                "Error while populating schematic blocks for WorldEdit building: $this",
                ex
            )
        }
    }

    override fun prepare(settings: BuildingSettings) {
        super.prepare(settings)
        this.timeBetweenBlocks = this.getDuration().dividedBy(this.blocks.size.toLong())
    }

    override fun finish() {
        super.finish()
        stopTask()
    }

    override fun finishInstantly() {
        super.prefinishInstantly()

        // The task cancels itself and calls finish() we don't need to do anything else.
        // Should we be worried about concurrent run() from here and the scheduled task?
        val task = blockChangeTask!!
        while (!task.isCancelled()) task.run()
    }

    override fun onTimerFinish(): Boolean = finishedBuilding

    override fun asFinishedBuilding(): Building {
        prepareAsFinished()
        return WorldEditBuilding(schematic, getOrigin(), getFacing(), getSettings())
    }

    enum class SortingStrategy : Comparator<BlockVector3> {
        TOP_TO_BOTTOM {
            override fun compare(first: BlockVector3, second: BlockVector3): Int {
                first.y.compareTo(second.y).takeIf { it != 0 }?.let { return it * -1 }
                first.x.compareTo(second.x).takeIf { it != 0 }?.let { return it }
                first.z.compareTo(second.z).takeIf { it != 0 }?.let { return it }
                return 0
            }
        },
        BOTTOM_TO_TOP {
            override fun compare(first: BlockVector3, second: BlockVector3): Int {
                first.y.compareTo(second.y).takeIf { it != 0 }?.let { return it }
                first.x.compareTo(second.x).takeIf { it != 0 }?.let { return it }
                first.z.compareTo(second.z).takeIf { it != 0 }?.let { return it }
                return 0
            }
        };
    }

    private val BlockVector3.block: Block
        get() {
            val bukkitLoc = BukkitAdapter.adapt(this.inWorld(getOrigin().world))
            return world.getBlockAt(bukkitLoc)
        }

    private abstract inner class BlockTask : Runnable {
        var done = false
        var failed = false

        fun done() {
            this@WorldEditBuildingConstruction.finishedBuilding = true
            this@WorldEditBuildingConstruction.finish()
            this.done = true
        }

        fun fail(ex: Throwable) {
            done()
            this.failed = true
            this@WorldEditBuildingConstruction.listeners.keys.forEach { it.onError(ex) }
        }
    }

    private inner class BlockPlaceTask : BlockTask() {
        init {
            failed = !SchematicManager.IS_SUPPORTED_VERSION || SchematicManager.UNSUPPORTED_FAWE
        }

        val indexedBlocks: Iterator<FunctionalWorldEditExtentBlock> = RangedIterator.skipped(
            ClipboardBlockCopyIterator(transformedClipboard, getOrigin(), sortingStrategy, true, true),
            Math.max(0, blockIndex)
        )

        override fun run() {
            if (done) return
            if (failed) {
                // This is reached when it wasn't supported in the first place.
                val msg = if (!SchematicManager.IS_SUPPORTED_VERSION) "Unsupported server version for WorldEdit"
                else if (SchematicManager.UNSUPPORTED_FAWE) "FAWE is not supported"
                else "Unknown error"

                fail(WorldEditBuildingConstructionException(msg, null))
                return
            }

            // Just here to prevent unexpected errors.
            if (!indexedBlocks.hasNext()) {
                done()
                return
            }

            this@WorldEditBuildingConstruction.blockIndex++
            val next = indexedBlocks.next()
            val appliedSuccessfully = try {
                next.applyFunction()
            } catch (ex: Throwable) {
                if (SchematicManager.isUnsupportedVersionError(ex)) {
                    SchematicManager.warnUnsupported(ex)
                } else if (SchematicManager.isUnsupportedFAWEError(ex)) {
                    SchematicManager.warnUnsupportedFawe(ex)
                } else {
                    EngineHubAddon.INSTANCE.logger.severe("Failed to place building for: ${getType()}->${getState()} at origin ${getOrigin()}")
                    ex.printStackTrace()
                }

                fail(ex)
                return
            }

            if (!appliedSuccessfully) {
                // The docs say the return value may be inaccurate... WHAT?
                // https://github.com/EngineHub/WorldEdit/blob/f31c2e65ea7429b8812c0aca5297e2b575740119/worldedit-core/src/main/java/com/sk89q/worldedit/extent/OutputExtent.java#L43-L48
                KLogger.debug(Debugger, "Failed to place block ${next.block} for ${this@WorldEditBuildingConstruction}")
            }

            // Old code for placing blocks one by one. Doesn't support NBT data (such as skull textures) if used this way.
            // val key: BlockVector3 = next.key
            // val data = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(next.value)
            // val block: Block = key.block
            // block.setType(data.material, false)
            // block.blockData = data
            // block.state.update(true, false)

            val block: Block = next.absolutePosition.block
            this@WorldEditBuildingConstruction.listeners.keys.forEach {
                try {
                    it.onBlockChange(block)
                } catch (ex: Throwable) {
                    KLogger.error("An error occurred while calling onBlockChange for $it -> ${this@WorldEditBuildingConstruction}")
                    ex.printStackTrace()
                }
            }

            // Should be checked at last because if there are no blocks left and
            // the task's delay is for example 10secs, another 10secs is wasted before
            // the task is run again for this check.
            if (!indexedBlocks.hasNext()) done()
        }
    }

    private inner class BlockBreakTask : BlockTask() {
        val indexedBlocks: Iterator<Map.Entry<BlockVector3, WorldEditExtentBlock>> = RangedIterator.skipped(
            blocks.iterator(),
            Math.max(0, blockIndex)
        )

        override fun run() {
            if (done) return

            // Just here to prevent unexpected errors.
            if (!indexedBlocks.hasNext()) {
                done()
                return
            }

            if (failed) return

            this@WorldEditBuildingConstruction.blockIndex++
            val next = indexedBlocks.next().key
            val bukkitBlock: Block = next.block
            this@WorldEditBuildingConstruction.listeners.keys.forEach {
                try {
                    it.onBlockChange(bukkitBlock)
                } catch (ex: Throwable) {
                    EngineHubAddon.INSTANCE.logger.severe("An error occurred while calling onBlockChange for $it -> ${this@WorldEditBuildingConstruction}")
                    ex.printStackTrace()
                }
            }
            bukkitBlock.type = Material.AIR

            if (!indexedBlocks.hasNext()) done()
        }
    }

    fun getPastedBlocks(): Int = blockIndex + 1
    fun getRemainingBlocks(): Int = getTotalBlocks() - getPastedBlocks()
    fun getTotalBlocks(): Int = blocks.size

    override fun onChunkLoad(chunk: BlockVector2): Boolean {
        val areAllChunksLoaded = super.onChunkLoad(chunk)
        if (areAllChunksLoaded && blockChangeTask == null) startTask()
        return areAllChunksLoaded
    }

    override fun onChunkUnload(chunk: BlockVector2): Boolean {
        stopTask()
        return super.onChunkUnload(chunk)
    }

    fun stopTask() {
        if (this.blockChangeTask == null) return
        this.blockChangeTask!!.cancel()
        this.blockChangeTask = null
    }

    fun startTask() {
        require(this.blockChangeTask == null) { "Block change task already started" }
        try {
            val instant = this.isInstant()
            val scheduler = if (instant) InstantTaskScheduler.INSTANCE else Kingdoms.taskScheduler().sync()
            val blockBuildTask =
                if (this.getType() == BuildingConstructionType.DEMOLISHING) BlockBreakTask() else BlockPlaceTask()
            this.blockChangeTask = scheduler.repeating(timeBetweenBlocks, timeBetweenBlocks, blockBuildTask)

            if (instant) {
                val task = this.blockChangeTask!!
                while (!task.isCancelled()) task.run()
            }
        } catch (ex: Throwable) {
            throw IllegalStateException("Error while pasting schematic: ${schematic.name} at ${getOrigin()}", ex)
        }
    }
}