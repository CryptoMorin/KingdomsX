package org.kingdoms.services.pets;

import org.bukkit.entity.Entity;
import org.kingdoms.services.Service;

import java.util.UUID;

public interface ServicePet extends Service {
    UUID getOwner(Entity entity);
}
