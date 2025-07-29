package org.kingdoms.versioning

import com.cryptomorin.xseries.reflection.XReflection
import java.util.logging.Logger

/**
 * This enum should not be sorted.
 * https://api.papermc.io/docs/swagger-ui/index.html?configUrl=/openapi/swagger-config doesn't really help.
 */
@Suppress("EnumEntryName")
enum class LatestMCVersion(val latestBuild: Int) {
    v1_8(445),
    v1_9(775),
    v1_10(918),
    v1_11(1106),
    v1_12(1620),
    v1_13(657),
    v1_14(245),
    v1_15(393),
    v1_16(794),
    v1_17(411),
    v1_18(388),
    v1_19(550),
    v1_20(151),
    v1_21(15),
    ;

    private val minorNumber: Int = ordinal + 8
    val latestPatchNumber: Int = XReflection.getLatestPatchNumberOf(minorNumber)!!
    val version = SemVer.of(1, minorNumber, latestPatchNumber)

    fun getLatestPaperBuildURL(): String {
        val fullVersion = version.asString(prefix = false, false)
        return "https://api.papermc.io/v2/projects/paper/versions/${fullVersion}/builds/${latestBuild}/downloads/" +
                "paper-${fullVersion}-${latestBuild}.jar";
    }

    fun ensureLatestPatch(logger: Logger): RuntimeException? {
        if (XReflection.PATCH_NUMBER < latestPatchNumber &&
            !ALLOWED_OUTDATED_VERSIONS.contains(CURRENT_VERSION)
        ) {
            logger.severe(
                "Your server is running an outdated patch of your current Minecraft version: "
                        + XReflection.getVersionInformation()
                        + " You need to download the latest patch (v1." + XReflection.MINOR_NUMBER + '.' + latestPatchNumber
                        + ") which you can download from ${getLatestPaperBuildURL()} directly. Because the plugin will not function properly with the older patches."
            )
            return IllegalStateException("Unsupported server version")
        }

        return null
    }

    companion object {
        @JvmField val CURRENT_VERSION: SemVer =
            SemVer.of(1, XReflection.MINOR_NUMBER, XReflection.PATCH_NUMBER)
        @JvmField val CURRENT_MINOR_VERSION: LatestMCVersion? =
            LatestMCVersion.values().find { x -> XReflection.MINOR_NUMBER == x.minorNumber }
        @JvmStatic val ALLOWED_OUTDATED_VERSIONS: Set<SemVer> =
            hashSetOf(
                SemVer.of(1, 20, 1),
                SemVer.of(1, 20, 2),
                SemVer.of(1, 20, 4),
                SemVer.of(1, 21, 0),
                SemVer.of(1, 21, 1),
                SemVer.of(1, 21, 3),
                SemVer.of(1, 21, 4),
                SemVer.of(1, 21, 5),
                SemVer.of(1, 21, 6),
                SemVer.of(1, 21, 7)
            )
    }
}