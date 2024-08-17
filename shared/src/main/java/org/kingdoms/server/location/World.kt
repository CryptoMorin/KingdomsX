package org.kingdoms.server.location

import java.util.*

interface World {
    val name: String
    val id: UUID
    val maxHeight: Int
    val minHeight: Int
}