package org.kingdoms.services.pets;

import fr.nocsy.mcpets.data.Pet;
import org.bukkit.entity.Entity;

public final class ServiceMCPets implements ServicePet {
    @Override
    public PetInfo getPetInfo(Entity entity) {
        Pet pet = Pet.getFromEntity(entity);
        if (pet == null) return null;
        return new PetInfo(pet.getOwner(), !pet.isInvulnerable());
    }
}
