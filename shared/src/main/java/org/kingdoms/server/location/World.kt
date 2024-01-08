package org.kingdoms.server.location

import java.util.UUID

interface World {
    fun getName(): String
    fun getId(): UUID
}