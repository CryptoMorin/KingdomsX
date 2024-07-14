package org.kingdoms.versioning

/**
 * https://semver.org/
 */
open class SemVer(originalString: String, parts: List<VersionPart>) : AbstractVersion(originalString, parts) {
    val majorVersion: Int = (parts[0] as VersionPart.Numeric).number
    val minorVersion: Int = if (parts.size > 1) (parts[1] as VersionPart.Numeric).number else 0
    val patchVersion: Int = if (parts.size > 2) (parts[2] as VersionPart.Numeric).number else 0

    override fun toString(): String {
        return "Version(majorVersion=$majorVersion, minorVersion=$minorVersion, patchVersion=$patchVersion, ${getOriginalString()})"
    }

    companion object {
        /**
         * https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
         */
        private val REGEX = Regex(
            "^(?<version>(?:(?:\\d+\\.)+)?\\d+)" +
                    "(?:-(?<prerelease>(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?" +
                    "(?:\\+(?<buildmetadata>[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?\$"
        )

        @JvmStatic fun of(string: String): SemVer {
            val matcher = REGEX.matchEntire(string) ?: throw IllegalArgumentException("Unknown version string: $string")
            val version = matcher.groups["version"]!!
            val prerelease = matcher.groups["prerelease"]
            val buildmetadata = matcher.groups["buildmetadata"]

            val parts: MutableList<VersionPart> = mutableListOf()
            parts.addAll(version.value.split('.').map { it.toInt() }.map { VersionPart.Numeric(it) })

            prerelease?.let { pr ->
                parts.addAll(pr.value.split('.').map { Version.parsePart(it) })
            }

            buildmetadata?.let { build ->
                parts.addAll(build.value.split('.').map { Version.parsePart(it) })
            }

            return SemVer(string, parts)
        }

        @JvmStatic fun of(majorVersion: Int, minorVersion: Int, patchVersion: Int): SemVer =
            SemVer(
                "v$majorVersion.$minorVersion.$patchVersion",
                listOf(
                    VersionPart.Numeric(majorVersion),
                    VersionPart.Numeric(minorVersion),
                    VersionPart.Numeric((patchVersion))
                )
            )
    }
}