package org.kingdoms.nbt

import org.kingdoms.nbt.tag.NBTTag
import org.kingdoms.nbt.tag.NBTTagType

interface NBTConverter<out T : NBTTag<*>, C> {
    val type: NBTTagType<@UnsafeVariance T>
    fun fromNBT(tag: C): T
    fun toNBT(tag: @UnsafeVariance T): C
}