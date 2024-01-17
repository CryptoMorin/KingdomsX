package org.kingdoms.server.platform

open class MinecraftVersion(val majorVersion: Int, val minorVersion: Int, val patchVersion: Int) {
    init {
        require(majorVersion > 0) { "Major number must be greater than 0: $majorVersion" }
        require(minorVersion >= 0) { "Minor number cannot be a negative number: $minorVersion" }
        require(patchVersion >= 0) { "Patch number cannot be a negative number: $patchVersion" }
    }

    fun asString(prefix: Boolean = true, separator: String = "."): String {
        return (if (prefix) "v" else "") +
                "$majorVersion$separator$minorVersion$separator$patchVersion"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is MinecraftVersion) return false;
        return this.majorVersion == other.majorVersion &&
                this.minorVersion == other.minorVersion &&
                this.patchVersion == other.patchVersion;
    }

    override fun hashCode(): Int {
        var result = (majorVersion xor (majorVersion ushr 32))
        result = 31 * result + (minorVersion xor (minorVersion ushr 32))
        result = 31 * result + (patchVersion xor (patchVersion ushr 32))
        return result
    }

    override fun toString(): String {
        return "MinecraftVersion(majorVersion=$majorVersion, minorVersion=$minorVersion, patchVersion=$patchVersion)"
    }

    companion object {
        @JvmStatic fun of(majorVersion: Int, minorVersion: Int, patchVersion: Int): MinecraftVersion =
            MinecraftVersion(majorVersion, minorVersion, patchVersion)
    }
}