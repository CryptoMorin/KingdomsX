package org.kingdoms.enginehub.worldedit

import com.sk89q.worldedit.Vector
import com.sk89q.worldedit.extent.clipboard.Clipboard
import org.kingdoms.server.location.BlockVector3

private class Clipboardv6(private val clipboard: Clipboard) : XClipboard {
    override val minimumPoint: BlockVector3
        get() = WorldEditBukkitAdapterv6.adaptBlockVector(clipboard.minimumPoint)

    override val maximumPoint: BlockVector3
        get() = WorldEditBukkitAdapterv6.adaptBlockVector(clipboard.maximumPoint)

    override val dimensions: BlockVector3
        get() = WorldEditBukkitAdapterv6.adaptBlockVector(clipboard.dimensions)

    override var origin: BlockVector3
        get() = WorldEditBukkitAdapterv6.adaptBlockVector(clipboard.origin)
        set(value) {
            clipboard.origin = Vector(value.x, value.y, value.z)
        }
}