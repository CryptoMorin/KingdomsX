package org.kingdoms.admintools.claimschematic

import org.kingdoms.constants.land.location.SimpleChunkLocation
import org.kingdoms.data.Pair
import org.kingdoms.server.location.BlockVector2
import org.kingdoms.utils.config.ConfigSection
import org.kingdoms.utils.config.adapters.YamlContainer
import org.kingdoms.utils.config.adapters.YamlParseContext
import org.kingdoms.utils.fs.FolderRegistry
import org.kingdoms.utils.internal.reflection.Reflect
import org.snakeyaml.api.Dump
import org.snakeyaml.api.DumpSettings
import org.snakeyaml.api.SimpleWriter
import org.snakeyaml.common.FlowStyle
import org.snakeyaml.common.ScalarStyle
import org.snakeyaml.nodes.*
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.stream.Collectors
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class ClaimSchematic(val name: String, val file: Path?, val layout: Set<BlockVector2>) {
    fun normalize(center: SimpleChunkLocation): Set<SimpleChunkLocation> {
        return this.layout
            .stream()
            .map { center.getRelative(it.x, it.z) }
            .collect(Collectors.toSet())
    }

    /**
     * Rotates a point around a center by the given angle (in radians).
     */
    private fun rotate(point: BlockVector2, center: BlockVector2, theta: Double): BlockVector2 {
        val dx: Int = point.x - center.x
        val dz: Int = point.z - center.z

        val xPrime: Double = dx * cos(theta) - dz * sin(theta)
        val zPrime: Double = dx * sin(theta) + dz * cos(theta)

        val rotatedX = (center.x + xPrime).roundToInt()
        val rotatedZ = (center.z + zPrime).roundToInt()

        return BlockVector2.of(rotatedX, rotatedZ)
    }

    fun rotate(center: BlockVector2, theta: Double): ClaimSchematic {
        val newLayout = layout.map { rotate(it, center, theta) }.toSet()
        return ClaimSchematic(name, file, newLayout)
    }

    override fun toString(): String = Reflect.toString(this, true)
}

class ClaimSchematicRegistry(folder: Path) : FolderRegistry("Claim Schematics", folder) {
    companion object {
        const val EXTENSION = "yml"
    }

    val schematics = HashMap<String, ClaimSchematic>()

    override fun getDefaultsURI(): Pair<String, URI>? = null

    fun getSchematic(name: String) = schematics[name]

    override fun handle(entry: Entry) {
        val schematicFile = entry.path
        val schematicName = entry.name
        schematics[schematicName] = loadSchematic(schematicName, schematicFile)
    }

    fun loadSchematic(name: String, file: Path): ClaimSchematic {
        val container = YamlContainer.parse(
            YamlParseContext()
                .named("Claim Schematic $file")
                .stream(file.inputStream(StandardOpenOption.READ))
                .excludeMarkers(true).excludeComments(true)
        )
        val section = ConfigSection(container)
        val layout = section.getNode("layout") as SequenceNode
        val layoutSet = HashSet<BlockVector2>()

        for (node in layout.value) {
            val coords = node as SequenceNode
            if (coords.value.size != 2) throw IllegalArgumentException("Claim schematics contains invalid coordinates: $coords")
            val xStr = coords.value[0] as ScalarNode
            val zStr = coords.value[1] as ScalarNode

            val x = xStr.value.toInt()
            val z = zStr.value.toInt()
            val vector = BlockVector2.of(x, z)

            layoutSet.add(vector)
        }

        return ClaimSchematic(name, file, layoutSet)
    }

    fun saveSchematic(name: String, layout: Set<BlockVector2>): ClaimSchematic {
        val file = this.folder.resolve("$name.$EXTENSION")
        val schematic = ClaimSchematic(name, file, layout)

        val root = MappingNode()
        val section = ConfigSection(root)

        val coords: List<Node> = layout.map {
            val x = ScalarNode(Tag.INT, it.x.toString(), ScalarStyle.PLAIN)
            val z = ScalarNode(Tag.INT, it.z.toString(), ScalarStyle.PLAIN)

            SequenceNode(Tag.SEQ, listOf(x, z), FlowStyle.FLOW)
        }.toList()
        val sectionLayout = SequenceNode(Tag.SEQ, coords, FlowStyle.BLOCK)

        section.set("version", 1)
        section.set("layout", sectionLayout)

        Files.createDirectories(folder)
        val dumper = Dump(DumpSettings())
        Files.newBufferedWriter(
            file,
            StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
        ).use { writer ->
            dumper.dumpNode(root, SimpleWriter(writer))
        }

        this.schematics[name] = schematic
        return schematic
    }

    override fun register() {
        if (folder.exists()) {
            visitPresent()
            if (useDefaults) visitDefaults()
        } else {
            if (useDefaults) visitDefaults()
        }
    }
}