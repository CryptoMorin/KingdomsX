package org.kingdoms.versioning

open class AbstractVersion(private val originalString: String, private val parts: List<VersionPart>) : Version {
    init {
        require(parts.isNotEmpty()) { "Version is empty" }
    }

    override fun getParts(): List<VersionPart> = this.parts
    override fun compareTo(other: Version): Int {
        val otherParts = other.getParts()
        val size = Math.min(parts.size, otherParts.size)
        for (i in 0..<size) {
            val part = parts[i]
            val otherPart = otherParts[i]
            val compareTo = part.compareTo(otherPart)
            if (compareTo != 0) return compareTo
        }

        if (parts.size == otherParts.size) return 0

        // Handles cases like:
        // 1.0.0.0.0.0 = 1.0.0
        // 1.0.0.1 > 1.0
        // 1.0.0.0.1-ALPHA > 1.0.0

        if (otherParts.size > parts.size) {
            val lastOfFirst = parts.last()
            val restOfSecond = otherParts.subList(parts.size - 1, otherParts.size /* exclusive */)
            restOfSecond.map { lastOfFirst.compareTo(it) }.find { it != 0 }?.let { return it }
            return 0
        } else {
            val lastOfSecond = otherParts.last()
            val restOfFirst = parts.subList(otherParts.size - 1, parts.size /* exclusive */)
            restOfFirst.map { it.compareTo(lastOfSecond) }.find { it != 0 }?.let { return it }
            return 0
        }
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