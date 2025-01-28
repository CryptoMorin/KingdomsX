package org.kingdoms;

import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.managers.entity.types.KingdomEntity;
import org.kingdoms.outposts.OutpostEvent;

public final class OutpostKingdomEntity extends KingdomEntity {
    private final OutpostEvent event;

    public OutpostKingdomEntity(@NonNull Entity entity, OutpostEvent event) {
        super(entity, null);
        this.event = event;
    }

    public OutpostEvent getEvent() {
        return event;
    }
}
