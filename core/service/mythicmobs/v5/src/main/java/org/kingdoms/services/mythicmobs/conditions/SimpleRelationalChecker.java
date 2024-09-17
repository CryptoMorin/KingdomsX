package org.kingdoms.services.mythicmobs.conditions;

import org.bukkit.entity.Entity;

public interface SimpleRelationalChecker {
    boolean check(Entity caster, Entity target);
}
