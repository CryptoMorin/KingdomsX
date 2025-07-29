package org.kingdoms.enginehub.worldedit

import com.sk89q.worldedit.extent.clipboard.Clipboard
import org.kingdoms.server.location.BlockVector3

private class Clipboardv7(private val clipboard: Clipboard) : XClipboard {
    override val minimumPoint: BlockVector3
        get() = WorldEditBukkitAdapterv7.adapt(clipboard.minimumPoint)

    override val maximumPoint: BlockVector3
        get() = WorldEditBukkitAdapterv7.adapt(clipboard.maximumPoint)

    override val dimensions: BlockVector3
        get() = WorldEditBukkitAdapterv7.adapt(clipboard.dimensions)

    override var origin: BlockVector3
        get() = WorldEditBukkitAdapterv7.adapt(clipboard.origin)
        set(value) {
            clipboard.origin = com.sk89q.worldedit.math.BlockVector3.at(value.x, value.y, value.z)
        }
}