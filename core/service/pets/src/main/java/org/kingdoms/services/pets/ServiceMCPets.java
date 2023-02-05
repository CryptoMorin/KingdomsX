package org.kingdoms.services.pets;

import fr.nocsy.mcpets.data.Pet;
import org.bukkit.entity.Entity;

import java.util.UUID;

public final class ServiceMCPets implements ServicePet {
    @Override
    public UUID getOwner(Entity entity) {
        Pet pet = Pet.getFromEntity(entity);
        return pet == null ? null : pet.getOwner();
    }
}
