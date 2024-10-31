package org.kingdoms.services.pets;

import org.bukkit.entity.Entity;
import simplepets.brainsynder.api.entity.IEntityPet;
import simplepets.brainsynder.api.plugin.SimplePets;

public final class ServiceSimplePets implements ServicePet {
    // https://github.com/brainsynder-Dev/SimplePets/blob/e3759832f8308857dab0e0ec5a691bcf683c71e2/plugin-api/src/simplepets/brainsynder/api/entity/IEntityPet.java#L36
    // https://github.com/brainsynder-Dev/SimplePets/blob/e3759832f8308857dab0e0ec5a691bcf683c71e2/plugin-api/src/simplepets/brainsynder/api/ISpawnUtil.java#L64
    // https://github.com/brainsynder-Dev/SimplePets/blob/e3759832f8308857dab0e0ec5a691bcf683c71e2/plugin-api/src/simplepets/brainsynder/api/plugin/IPetsPlugin.java#L46-L49

    @Override
    public PetInfo getPetInfo(Entity entity) {
        return SimplePets.getSpawnUtil().getHandle(entity)
                .filter(x -> x instanceof IEntityPet)
                .map(x -> (IEntityPet) x)
                .map(x -> new PetInfo(x.getOwnerUUID(), x.isPetVisible()))
                .orElse(null);
    }
}
