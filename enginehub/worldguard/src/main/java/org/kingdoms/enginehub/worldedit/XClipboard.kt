package org.kingdoms.enginehub.worldedit

import org.kingdoms.server.location.BlockVector3

interface XClipboard {
    val minimumPoint: BlockVector3

    val maximumPoint: BlockVector3

    val dimensions: BlockVector3

    var origin: BlockVector3
}