package org.kingdoms.peacetreaties.events;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.peacetreaties.data.PeaceTreaty;

public final class PeaceTreatyEndEvent extends PeaceTreatyEvent {
    private final Reason reason;

    public PeaceTreatyEndEvent(PeaceTreaty peaceTreaty, Reason reason) {
        super(peaceTreaty);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
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

    public enum Reason {
        PROPOSER_DISBANDED, DURATION
    }
}
