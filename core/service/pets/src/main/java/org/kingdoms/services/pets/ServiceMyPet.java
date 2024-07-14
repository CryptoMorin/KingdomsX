package org.kingdoms.services.pets;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import org.bukkit.entity.Entity;

public final class ServiceMyPet implements ServicePet {
    @Override
    public PetInfo getPetInfo(Entity entity) {
        if (entity instanceof MyPetBukkitEntity) {
            MyPet pet = ((MyPetBukkitEntity) entity).getMyPet();
            return new PetInfo(pet.getOwner().getPlayerUUID(), true);
        } else {
            return null;
        }
    }
}
