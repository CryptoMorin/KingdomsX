package org.kingdoms.peacetreaties.events;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.peacetreaties.data.PeaceTreaty;

public final class PeaceTreatyStartEvent extends PeaceTreatyEvent {
    public PeaceTreatyStartEvent(PeaceTreaty peaceTreaty) {
        super(peaceTreaty);
    }

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }
}
