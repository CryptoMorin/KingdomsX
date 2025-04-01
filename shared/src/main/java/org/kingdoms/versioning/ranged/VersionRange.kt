package org.kingdoms.versioning.ranged

import org.kingdoms.versioning.SemVer
import org.kingdoms.versioning.Version

/**
 * The comparator tries to sort version ranges based on their version order.
 * This is an estimate comparision in some cases since version ranges can overlap.
 */
interface VersionRange : Comparable<VersionRange> {
    val originalString: String

    fun isIncluded(version: Version): Boolean

    companion object {
        @JvmStatic fun of(range: String): VersionRange {
            val trimmed = range.trim()
            if (trimmed.isEmpty()) throw IllegalArgumentException("Not a version range: '$range'")

            val rangeIndex = trimmed.indexOf("...")

            if (rangeIndex != -1) {
                val leftSide = trimmed.substring(0, rangeIndex).trim()
                val rightSide = trimmed.substring(rangeIndex + 3).trim()

                return EnclosedVersionRange(SemVer.of(leftSide), SemVer.of(rightSide), range)
            }

            val specialCharPredicate: (Char) -> Boolean = { it == '<' || it == '>' || it == '=' }

            val firstOp = trimmed.takeWhile(specialCharPredicate)
            val midOperand = trimmed.substring(firstOp.length).takeWhile { !specialCharPredicate.invoke(it) }
            val secondOp = trimmed.substring(firstOp.length + midOperand.length).takeWhile(specialCharPredicate)

            if (firstOp.isNotEmpty() && secondOp.isNotEmpty()) {
                // >x>
                throw IllegalArgumentException("Too many operators for version range: '$range'")
            }
            if (midOperand.isEmpty()) {
                throw IllegalArgumentException("No operand: '$range'")
            }

            val midVer = SemVer.of(midOperand.trim())
            if (firstOp.isNotEmpty()) {
                return when (firstOp) {
                    "<" -> LessThanVersionRange(midVer, orEqual = false, range)
                    "<=" -> LessThanVersionRange(midVer, orEqual = true, range)
                    ">" -> GreaterThanVersionRange(midVer, orEqual = false, range)
                    ">=" -> GreaterThanVersionRange(midVer, orEqual = true, range)
                    else -> error("Unknown op '$firstOp' for version range: '$range'")
                }
            }
            if (secondOp.isNotEmpty()) {
                return when (secondOp) {
                    "<" -> GreaterThanVersionRange(midVer, orEqual = false, range)
                    "<=" -> GreaterThanVersionRange(midVer, orEqual = true, range)
                    ">" -> LessThanVersionRange(midVer, orEqual = false, range)
                    ">=" -> LessThanVersionRange(midVer, orEqual = true, range)
                    else -> error("Unknown op '$secondOp' for version range: '$range'")
                }
            }

            return ExactVersionRange(SemVer.of(range))
        }
    }
}

abstract class AbstractVersionRange(override val originalString: String) : VersionRange {
    override fun toString(): String = this.javaClass.simpleName + "($originalString)"
}

/**
 * Single entry version range.
 */
class ExactVersionRange(val version: Version) : AbstractVersionRange(version.getOriginalString()) {
    override fun isIncluded(version: Version): Boolean {
        return this.version == version
    }

    override fun compareTo(other: VersionRange): Int =
        when (other) {
            is ExactVersionRange -> this.version.compareTo(other.version)

            is EnclosedVersionRange -> {
                if (other.isIncluded(this.version)) 0
                else this.version.compareTo(other.least)
            }

            is LessThanVersionRange -> {
                if (this.version >= other.version) +1
                else if (other.isIncluded(this.version)) 0
                else -1
            }

            is GreaterThanVersionRange -> {
                if (this.version >= other.version) +1
                else -1
            }

            else -> throw UnsupportedOperationException("Unknown version range: $other")
        }
}

/**
 * Version range consisting of an inclusive [least] and an exclusive [most].
 */
class EnclosedVersionRange(val least: Version, val most: Version, originalString: String) :
    AbstractVersionRange(originalString) {

    override fun isIncluded(version: Version): Boolean = version in least..<most

    /**
     * - `1.17.2 ... 1.17.6` **<=>** `1.17.2 ... 1.17.6` **->** 0
     * - `1.17.2 ... 1.17.10` **<=>** `1.17.4 ... 1.17.6` **->** 1
     * - `1.17.1 ... 1.17.5` **<=>** `1.17.6 ... 1.17.10` **->** -1
     * - `1.17.1 ... 1.17.5` **<=>** `1.17.3 ... 1.17.6` **->** -1 (based on the min version)
     * ---
     * - `1.17.1 ...` **<=>** `1.17.3 ...` **->** -1
     * - `... 1.17.1` **<=>** `... 1.17.3` **->** -1
     * - `1.17.1 ...` **<=>** `... 1.17.3` **->** 0
     */
    override fun compareTo(other: VersionRange): Int =
        when (other) {
            is ExactVersionRange -> {
                if (this.isIncluded(other.version)) 0
                else this.least.compareTo(other.version)
            }

            is LessThanVersionRange -> {
                if (this.most == other.version) {
                    if (other.orEqual) -1 else 0
                } else if (this.most < other.version) -1
                else if (this.most > other.version) +1
                else if (this.least < other.version) -1
                else if (this.least > other.version) +1
                else 0
            }

            is GreaterThanVersionRange -> -1

            is EnclosedVersionRange -> {
                if (this.least == other.least && this.most == other.most) 0
                else if (this.most > other.most) +1
                else if (this.most < other.most) -1
                else if (this.least > other.least) +1
                else if (this.least < other.least) -1
                else 0
            }

            else -> throw UnsupportedOperationException("Unknown version range: $other")
        }
}

class LessThanVersionRange(val version: Version, val orEqual: Boolean, originalString: String) :
    AbstractVersionRange(originalString) {

    override fun isIncluded(version: Version): Boolean =
        if (orEqual) version <= this.version
        else version < this.version

    override fun compareTo(other: VersionRange): Int =
        when (other) {
            // @formatter:off
            is ExactVersionRange -> {
                val cmp = this.version.compareTo(other.version)

                if (cmp == 0 && !orEqual) -1 // 1.2 <= 1.2
                else if (cmp > 0)          1 // 1.2 <= 1.3
                else if (cmp < 0)         -1 // 1.3 <= 1.2
                else                       0 // 1.2 <= 1.2 (cmp == 0 && orEqual)
            }

            is LessThanVersionRange -> {
                if (this.version == other.version) {
                    if (this.orEqual == other.orEqual) 0
                    else if (this.orEqual) 1
                    else -1
                } else {
                    if (this.version < other.version) -1
                    else +1
                }
            }

            is GreaterThanVersionRange -> -1

            is EnclosedVersionRange -> {
                if (this.version == other.most) {
                    if (this.orEqual) 0 else -1
                } else if (this.version > other.most)+1
                else if (this.version < other.most)  -1
                else if (this.version < other.least) -1
                else if (this.version > other.most)   1
                else                                  0 // 1.2 ... 1.5 | <1.4 (aka isIncluded)
            }

            else -> throw UnsupportedOperationException("Unknown version range: $other")
            // @formatter:on
        }
}

class GreaterThanVersionRange(val version: Version, val orEqual: Boolean, originalString: String) :
    AbstractVersionRange(originalString) {

    override fun isIncluded(version: Version): Boolean =
        if (orEqual) version >= this.version
        else version > this.version

    override fun compareTo(other: VersionRange): Int =
        when (other) {
            // @formatter:off
            is GreaterThanVersionRange -> {
                if (this.version == other.version) {
                    if (this.orEqual == other.orEqual) 0
                    else if (this.orEqual) +1
                    else -1
                }
                else this.version.compareTo(other.version)
            }

            is ExactVersionRange -> {
                if (other.version >= this.version) -1
                else +1
            }

            is LessThanVersionRange, is EnclosedVersionRange -> +1

            else -> throw UnsupportedOperationException("Unknown version range: $other")
            // @formatter:on
        }
}
