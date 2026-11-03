package org.kingdoms.utils.paper;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

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

    public static final class EntityLoadCrossbowEvent {
        private final io.papermc.paper.event.entity.EntityLoadCrossbowEvent event;

        public EntityLoadCrossbowEvent(io.papermc.paper.event.entity.EntityLoadCrossbowEvent event) {this.event = event;}

        public @NotNull LivingEntity getEntity() {
            return event.getEntity();
        }

        public boolean isCancelled() {
            return event.isCancelled();
        }

        public @NotNull EquipmentSlot getHand() {
            return event.getHand();
        }

        public boolean shouldConsumeItem() {
            return event.shouldConsumeItem();
        }

        public void setCancelled(boolean cancel) {
            event.setCancelled(cancel);
        }

        public @NotNull EntityType getEntityType() {
            return event.getEntityType();
        }

        public void setConsumeItem(boolean consume) {
            event.setConsumeItem(consume);
        }

        public @Nullable ItemStack getCrossbow() {
            return event.getCrossbow();
        }
    }

    public static class PaperEventExecutor<PaperEvent> implements EventExecutor, Listener {
        private final Consumer<PaperEvent> eventHandle;

        public PaperEventExecutor(Consumer<PaperEvent> eventHandle) {this.eventHandle = eventHandle;}

        @SuppressWarnings("unchecked")
        @Override
        public void execute(@NotNull Listener listener, @NotNull Event event) throws EventException {
            eventHandle.accept((PaperEvent) event);
        }
    }

    public static void registerBeaconEffectEvent(Plugin plugin, Listener listener, EventPriority priority, Consumer<BeaconEffectEvent> eventHandle) {
        registerEvent(
                com.destroystokyo.paper.event.block.BeaconEffectEvent.class,
                new PaperEventExecutor<>(BeaconEffectEvent::new),
                plugin, listener, priority
        );
    }

    public static void registerEntityLoadCrossbowEvent(Plugin plugin, Listener listener, EventPriority priority, Consumer<EntityLoadCrossbowEvent> eventHandle) {
        registerEvent(
                io.papermc.paper.event.entity.EntityLoadCrossbowEvent.class,
                new PaperEventExecutor<>(EntityLoadCrossbowEvent::new),
                plugin, listener, priority
        );
    }

    private static <T extends Event> void registerEvent(Class<T> eventClass, PaperEventExecutor<T> executor, Plugin plugin, Listener listener, EventPriority priority) {
        Bukkit.getPluginManager().registerEvent(
                eventClass,
                listener,
                priority,
                executor,
                plugin,
                true
        );
    }

    public static boolean supportsBeaconEffectEvent() {
        return isSupported("com.destroystokyo.paper.event.block.BeaconEffectEvent");
    }

    public static boolean supportsEntityLoadCrossbowEvent() {
        return isSupported("io.papermc.paper.event.entity.EntityLoadCrossbowEvent");
    }

    private static boolean isSupported(String event) {
        try {
            Class.forName(event);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
