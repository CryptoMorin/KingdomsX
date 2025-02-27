package org.kingdoms.services.pets;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import su.nightexpress.combatpets.api.pet.ActivePet;
import su.nightexpress.combatpets.api.pet.PetEntityBridge;

public final class ServiceCombatPets implements ServicePet {
    @Override
    public PetInfo getPetInfo(Entity entity) {
        if (!(entity instanceof LivingEntity)) return null;
        ActivePet pet = PetEntityBridge.getByMob((LivingEntity) entity);
        if (pet == null) return null;
        return new PetInfo(pet.getOwner().getUniqueId(), true);
    }
}
