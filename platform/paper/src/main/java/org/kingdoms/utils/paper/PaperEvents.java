package org.kingdoms.utils.paper;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;

import java.util.function.Consumer;

public final class PaperEvents {
    public static final class BeaconEffectEvent {
        private final com.destroystokyo.paper.event.block.BeaconEffectEvent event;

        public BeaconEffectEvent(com.destroystokyo.paper.event.block.BeaconEffectEvent event) {this.event = event;}

        public Block getBlock() {
            return event.getBlock();
        }

        public void setCancelled(boolean cancelled) {
            event.setCancelled(cancelled);
        }

        public Player getPlayer() {
            return event.getPlayer();
        }

        public PotionEffect getEffect() {
            return event.getEffect();
        }
    }

    public static Listener create(Consumer<BeaconEffectEvent> beaconEffectEvent) {
        return new BeaconManager(beaconEffectEvent);
    }

    public static boolean supportsBeaconEffectEvent() {
        return isSupported("com.destroystokyo.paper.event.block.BeaconEffectEvent");
    }

    private static boolean isSupported(String event) {
        try {
            Class.forName(event);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static final class BeaconManager implements Listener {
        private final Consumer<BeaconEffectEvent> beaconEffectEvent;

        private BeaconManager(Consumer<BeaconEffectEvent> beaconEffectEvent) {this.beaconEffectEvent = beaconEffectEvent;}

        // https://jd.papermc.io/paper/1.21.4/com/destroystokyo/paper/event/block/BeaconEffectEvent.html
        @EventHandler(ignoreCancelled = true)
        public void onBeaconEffect(com.destroystokyo.paper.event.block.BeaconEffectEvent event) {
            if (beaconEffectEvent != null) {
                beaconEffectEvent.accept(new BeaconEffectEvent(event));
            }
        }
    }
}
