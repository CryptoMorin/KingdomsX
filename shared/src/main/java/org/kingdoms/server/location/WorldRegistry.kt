package org.kingdoms.server.location

import java.util.*

interface WorldRegistry {
    fun getWorld(name: String): World?
    fun getWorld(id: UUID): World?
}