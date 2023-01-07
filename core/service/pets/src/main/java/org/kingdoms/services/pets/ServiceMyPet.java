package org.kingdoms.services.pets;

import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import org.bukkit.entity.Entity;

import java.util.UUID;

public final class ServiceMyPet implements ServicePet {
    @Override
    public UUID getOwner(Entity entity) {
        if (entity instanceof MyPetBukkitEntity) {
            return ((MyPetBukkitEntity) entity).getMyPet().getOwner().getPlayerUUID();
        } else {
            return null;
        }
    }
}
