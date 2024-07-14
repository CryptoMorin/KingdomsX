package org.kingdoms.services.pets;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import su.nightexpress.combatpets.api.pet.IPetHolder;
import su.nightexpress.combatpets.pet.PetManager;

public final class ServiceCombatPets implements ServicePet {
    @Override
    public PetInfo getPetInfo(Entity entity) {
        if (!(entity instanceof LivingEntity)) return null;
        IPetHolder pet = PetManager.getPet((LivingEntity) entity);
        if (pet == null) return null;
        return new PetInfo(pet.getOwner().getUniqueId(), true);
    }
}
