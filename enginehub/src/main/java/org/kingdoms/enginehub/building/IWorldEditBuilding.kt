package org.kingdoms.enginehub.building

import org.kingdoms.constants.land.building.Building
import org.kingdoms.enginehub.schematic.WorldEditSchematic

interface IWorldEditBuilding : Building {
    val schematic: WorldEditSchematic
}