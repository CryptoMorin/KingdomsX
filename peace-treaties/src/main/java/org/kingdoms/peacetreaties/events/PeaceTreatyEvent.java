package org.kingdoms.peacetreaties.events;

import org.kingdoms.events.KingdomsEvent;
import org.kingdoms.peacetreaties.data.PeaceTreaty;

public abstract class PeaceTreatyEvent extends KingdomsEvent {
    private final PeaceTreaty peaceTreaty;

    public PeaceTreatyEvent(PeaceTreaty peaceTreaty) {
        this.peaceTreaty = peaceTreaty;
    }

    public final PeaceTreaty getPeaceTreaty() {
        return peaceTreaty;
    }
}
