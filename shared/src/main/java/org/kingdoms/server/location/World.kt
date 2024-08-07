package org.kingdoms.server.location

import java.util.*

interface World {
    fun getName(): String
    fun getId(): UUID
    fun getMaxHeight(): Int
    fun getMinHeight(): Int
}