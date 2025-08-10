package org.kingdoms.versioning

open class AbstractVersion(private val originalString: String, private val parts: List<VersionPart>) : Version {
    init {
        require(parts.isNotEmpty()) { "Version is empty" }
    }

    override fun getParts(): List<VersionPart> = this.parts
    override fun compareTo(other: Version): Int {
        val otherParts = other.getParts()
        val size = parts.size.coerceAtMost(otherParts.size)
        for (i in 0..<size) {
            val part = parts[i]
            val otherPart = otherParts[i]
            val compareTo = part.compareTo(otherPart)
            if (compareTo != 0) return compareTo
        }

        if (parts.size == otherParts.size) return 0

        // All components from the left are equal, so now decide what to do with excessive ones.
        val isThis = parts.size > otherParts.size
        val biggerVersion = if (isThis) parts else otherParts

        val lastNonZeroComponent = biggerVersion.drop(size).find { it !is VersionPart.Numeric || it.number != 0 }

        // 1.0.0.0.0.0 = 1.0.0
        if (lastNonZeroComponent === null) return 0

        // 1.0.1 > 1.0
        if (lastNonZeroComponent is VersionPart.Numeric) return if (isThis) +1 else -1

        // 1.0.0.0.1-ALPHA > 1.0.0
        // 1.0.0-ALPHA < 1.0.0
        if (lastNonZeroComponent is VersionPart.Stage && lastNonZeroComponent.type !== VersionPart.PreReleaseType.RELEASE)
            return if (isThis) -1 else +1

        // Unknown component
        return if (isThis) +1 else -1
    }

    override fun canBeComparedTo(other: Version): Boolean = true
    override fun getOriginalString(): String = originalString
    override fun toString(): String = "${this.javaClass.simpleName}(${getFriendlyString(false)} - ${getParts()})"

    override fun equals(other: Any?): Boolean {
        if (other !is Version) return false;
        val parts = this.getParts()
        val otherParts = other.getParts()

        if (parts.size != otherParts.size) return false
        for (i in parts.indices) {
            val part = parts[i]
            val otherPart = otherParts[i]
            if (part != otherPart) return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        var first = true

        for (part in parts) {
            val hash = part.hashCode()
            if (first) {
                result = (hash xor (hash ushr 32))
                first = false
            } else {
                result = 31 * result + (hash xor (hash ushr 32))
            }
        }

        return result
    }
}