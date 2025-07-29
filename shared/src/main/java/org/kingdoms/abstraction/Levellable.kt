package org.kingdoms.abstraction

import org.jetbrains.annotations.Range

/**
 * Anything that has levels, like turrets and structures.
 * But kingdom upgrades are not seperate objects, they exist as pure int.
 * Levels are always expected to start from 1 not 0, but some mechanism
 * might use zero or negative values to signify other states.
 */
interface Levellable {
    fun getLevel(): @Range(from = 0, to = Integer.MAX_VALUE.toLong()) Int
    fun setLevel(level: @Range(from = 0, to = Integer.MAX_VALUE.toLong()) Int)
}