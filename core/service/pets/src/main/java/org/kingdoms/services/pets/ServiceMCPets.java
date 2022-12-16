package org.kingdoms.services.pets;

import fr.nocsy.mcpets.data.Pet;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

public final class ServiceMCPets implements ServicePet {
    @Override
    @Nullable
    public UUID getOwner(@NonNull Entity entity) {
        Pet pet = Pet.getFromEntity(entity);
        return pet == null ? null : pet.getOwner();
    }
}
