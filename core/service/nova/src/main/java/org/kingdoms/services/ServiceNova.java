package org.kingdoms.services;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kingdoms.api.KingdomsAPI;
import org.kingdoms.api.KingdomsActionProcessor;
import org.kingdoms.platform.bukkit.adapters.BukkitAdapter;
import xyz.xenondevs.nova.api.Nova;
import xyz.xenondevs.nova.api.protection.ProtectionIntegration;

/**
 * https://github.com/xenondevs/Nova
 * https://github.com/xenondevs/Nova/tree/main/nova-hooks
 */
public final class ServiceNova implements Service, ProtectionIntegration {
    private static final KingdomsActionProcessor API = KingdomsAPI.getApi().getActionProcessor();

    @Override
    public void enable() {
        Nova.getNova().registerProtectionIntegration(this);
    }

    @Override
    public boolean canBreak(@NotNull OfflinePlayer offlinePlayer, @Nullable ItemStack itemStack, @NotNull Location location) {
        return API.canBreak(offlinePlayer, itemStack, BukkitAdapter.adapt(location));
    }

    @Override
    public boolean canPlace(@NotNull OfflinePlayer offlinePlayer, @NotNull ItemStack itemStack, @NotNull Location location) {
        return API.canPlace(offlinePlayer, itemStack, BukkitAdapter.adapt(location));
    }

    @Override
    public boolean canUseBlock(@NotNull OfflinePlayer offlinePlayer, @Nullable ItemStack itemStack, @NotNull Location location) {
        return API.canUseBlock(offlinePlayer, itemStack, BukkitAdapter.adapt(location));
    }

    @Override
    public boolean canUseItem(@NotNull OfflinePlayer offlinePlayer, @NotNull ItemStack itemStack, @NotNull Location location) {
        return API.canUseItem(offlinePlayer, itemStack, BukkitAdapter.adapt(location));
    }

    @Override
    public boolean canInteractWithEntity(@NotNull OfflinePlayer offlinePlayer, @NotNull Entity entity, @Nullable ItemStack itemStack) {
        return API.canInteractWithEntity(offlinePlayer, entity, itemStack);
    }

    @Override
    public boolean canHurtEntity(@NotNull OfflinePlayer offlinePlayer, @NotNull Entity entity, @Nullable ItemStack itemStack) {
        return true;
    }
}
