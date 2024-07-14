package org.kingdoms.nbt

import org.kingdoms.nbt.tag.NBTTag
import java.util.*

object NBTTagConverterRegistry {
    private val registry: MutableMap<NBTTagId, NBTConverter<NBTTag<*>, *>> = EnumMap(NBTTagId::class.java)

    fun register(converter: NBTConverter<NBTTag<*>, *>) {
        require(registry.put(converter.type.id(), converter) == null) {
            "Converter registered twice: ${converter.type} -> $converter"
        }
    }

    fun get(type: NBTTagId): NBTConverter<NBTTag<*>, *>? = registry[type]
}