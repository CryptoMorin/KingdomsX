package org.kingdoms.enginehub

import com.sk89q.worldedit.extent.clipboard.Clipboard
import org.kingdoms.enginehub.worldedit.XWorldEditBukkitAdapterFactory
import org.kingdoms.server.location.BlockPoint3D
import org.kingdoms.server.location.BlockVector3

object WorldEditAdapter {
    @JvmStatic
    @get:JvmName("adapt")
    val BlockPoint3D.adapt: com.sk89q.worldedit.math.BlockVector3
        get() = com.sk89q.worldedit.math.BlockVector3.at(x, y, z)

    val BlockVector3.adapt: com.sk89q.worldedit.math.BlockVector3
        get() = com.sk89q.worldedit.math.BlockVector3.at(x, y, z)

    @JvmStatic fun BlockVector3.getAbsolutePosition(
        origin: BlockVector3,
        clipboard: Clipboard,
    ): BlockVector3 {
        // Subtract from the origin, so we can get pure coordinates relative to
        // the 0,0,0 origin and add our other origin to it.
        return this.subtract(XWorldEditBukkitAdapterFactory.INSTANCE.adapt(clipboard).origin).add(origin)
    }
}