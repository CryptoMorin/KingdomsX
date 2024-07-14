package org.kingdoms.server.location

import org.kingdoms.utils.internal.Purifier

// Note: Don't do instance check for abstract classes
// Other classes can still inherit from those abstract classes and
// the purifier will think these are pure classes while they're not.

object PurifierImmutableBlockVector3 : Purifier<BlockVector3> {
    override fun purify(original: BlockVector3): BlockVector3 = BlockVector3.of(original)
}

object PurifierImmutableBlockVector2 : Purifier<BlockVector2> {
    override fun purify(original: BlockVector2): BlockVector2 = BlockVector2.of(original)
}