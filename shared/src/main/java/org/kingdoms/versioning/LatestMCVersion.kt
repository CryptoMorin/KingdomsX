package org.kingdoms.versioning

import com.cryptomorin.xseries.reflection.XReflection
import java.util.logging.Logger

/**
 * Minecraft latest patch numbers with link to the latest paper downloads.
 * This does not consider whether the server is using Spigot, Folia or other forks such as Purpur.
 * https://api.papermc.io/docs/swagger-ui/index.html?configUrl=/openapi/swagger-config doesn't really help.
 */
@Suppress("EnumEntryName")
enum class LatestMCVersion(private val paperBuildNumber: Int,
                           private val paperFileHash: String) {
    v1_8(445, "7ff6d2cec671ef0d95b3723b5c92890118fb882d73b7f8fa0a2cd31d97c55f86"),
    v1_9(775, "15a5821ddeacc596432c3fbf24262a2d264f556060ecd6f1838fb01ab5629a81"),
    v1_10(918, "83354d24a22b6265e76c089b3d17a568abb446c0ccd12c2452f5e148412b16c2"),
    v1_11(1106, "3d0f40ec1f9630dfdbafa626cc20c266d7fb90fc22583dc1b995e7fbfb76830d"),
    v1_12(1620, "3a2041807f492dcdc34ebb324a287414946e3e05ec3df6fd03f5b5f7d9afc210"),
    v1_13(657, "11e828d0565ab76a0a0e180c056364a95de44958cfd6a6af3f9b1dc70b03e9cd"),
    v1_14(245, "bd8ec5cdb22370d37816a6de26798df3d2b0d6f9c7c96c88ca45a1303fea50e8"),
    v1_15(393, "bd2dd6f2cc489cf9e2bb800cb4fb6d63e9d293945d3ac10b09dd9c6098fa9f34"),
    v1_16(794, "e67da4851d08cde378ab2b89be58849238c303351ed2482181a99c2c2b489276"),
    v1_17(411, "6cc1ee2f94253ce10b5374ed85fffc735a97d8f1b64db293683dfa24dd3cc05f"),
    v1_18(388, "0578f18f4d632b494b468ec56b3b414b5b56fea087ee7d39cf6dcdf4c9d01f05"),
    v1_19(550, "e587d78cba3e99ef8c4bc24cf20cc3bdbbe89e33b0b572070446af4eb6be5ccf"),
    v1_20(151, "4b011f5adb5f6c72007686a223174fce82f31aeb4b34faf4652abc840b47e640"),
    v1_21(132, "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba"),

    v26_1(74, "1d70b1dab9cf4a6de615209a536f3a45a2186240253c428213ce2188ab95e5f7"),
    v26_2(135, "ef3dbc68b013deafdbbee3bebf1ba351671ebcda94a8f136809b7532d5ba2fe1"),
    v26_3(231, "a836a016be7ac52352eb3a1efb6857bb42ee73a6c78d46e292f7e87742edb0e6"),
    ;

    val version: SemVer

    init {
        val split = this.name.split("_")
        val majorVersion = split[0].substring(1).toInt()
        val minorVersion = split[1].toInt()
        val patchNumber = XReflection.getLatestPatchNumberOf(majorVersion, minorVersion)!!

        version = SemVer.of(majorVersion, minorVersion, patchNumber)
    }

    fun getLatestPaperBuildURL(): String {
        val fullVersion = version.asString(prefix = false, false)

        val normalizedPatchVersion = if (version.patchVersion != 0) "." + version.patchVersion else ""
        val normalizedVersion = version.majorVersion.toString() + "." + version.minorVersion + normalizedPatchVersion
        return "https://fill-data.papermc.io/v1/objects/${paperFileHash}/paper-${normalizedVersion}-${paperBuildNumber}.jar"

        // Old way of doing it:
        // return "https://api.papermc.io/v2/projects/paper/versions/${fullVersion}/builds/${paperBuildNumber}/downloads/" +
        //         "paper-${fullVersion}-${paperBuildNumber}.jar";
    }

    fun ensureLatestPatch(logger: Logger): RuntimeException? {
        val currentPatch = XReflection.PATCH_NUMBER
        val latestPatch = version.patchVersion

        if (currentPatch < latestPatch &&
            !ALLOWED_OUTDATED_VERSIONS.contains(CURRENT_VERSION)
        ) {
            logger.severe(
                "Your server is running an outdated patch of your current Minecraft version: "
                        + XReflection.getVersionInformation()
                        + " You need to download the latest patch (" + version.asString(prefix = true, short = true)
                        + ") which you can download from ${getLatestPaperBuildURL()} directly. Because the plugin will not function properly with the older patches."
            )
            return IllegalStateException("Unsupported server version")
        }

        return null
    }

    companion object {
        @JvmField val CURRENT_VERSION: SemVer =
            SemVer.of(XReflection.MAJOR_NUMBER, XReflection.MINOR_NUMBER, XReflection.PATCH_NUMBER)
        @JvmField val CURRENT_MINOR_VERSION: LatestMCVersion? =
            LatestMCVersion.values().find { x -> XReflection.MAJOR_NUMBER == x.version.majorVersion && XReflection.MINOR_NUMBER == x.version.minorVersion }
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
                SemVer.of(1, 21, 7),

                SemVer.of(26, 1, 1)
            )
    }
}