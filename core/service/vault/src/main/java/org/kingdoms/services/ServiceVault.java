package org.kingdoms.services;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class ServiceVault implements Service {
    private static final Economy ECONOMY;
    private static final Permission PERMISSIONS;
    private static final Chat CHAT;

    static {
        RegisteredServiceProvider<Economy> economyRSP = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (economyRSP == null) ECONOMY = null;
        else ECONOMY = economyRSP.getProvider();

        RegisteredServiceProvider<Permission> permissionRSP = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (permissionRSP == null) PERMISSIONS = null;
        else PERMISSIONS = permissionRSP.getProvider();

        RegisteredServiceProvider<Chat> chatRSP = Bukkit.getServicesManager().getRegistration(Chat.class);
        if (chatRSP == null) CHAT = null;
        else CHAT = chatRSP.getProvider();
    }

    @Override
    public boolean isAvailable() {
        return ECONOMY != null || PERMISSIONS != null || CHAT != null;
    }

    public static boolean isAvailable(Component component) {
        switch (component) {
            case ECO:
                return ECONOMY != null;
            case CHAT:
                return CHAT != null;
            case PERM:
                return PERMISSIONS != null;
            default:
                throw new AssertionError("Unknown Vault component: " + component);
        }
    }

    public enum Component {ECO, CHAT, PERM;}

    public static double getMoney(OfflinePlayer player) {
        return ECONOMY == null ? 0 : ECONOMY.getBalance(player);
    }

    public static void deposit(OfflinePlayer player, double amount) {
        ECONOMY.depositPlayer(player, amount);
    }

    public static boolean hasMoney(OfflinePlayer player, double amount) {
        return ECONOMY != null && ECONOMY.has(player, amount);
    }

    public static String getDisplayName(Player player) {
        if (CHAT == null) return player.getDisplayName();
        String prefix = CHAT.getPlayerPrefix(player);
        String suffix = CHAT.getPlayerSuffix(player);
        return prefix + player.getName() + suffix;
    }

    public static void withdraw(OfflinePlayer player, double amount) {
        ECONOMY.withdrawPlayer(player, amount);
    }

    public static String getGroup(Player player) {
        if (PERMISSIONS == null) return "default";
        return PERMISSIONS.hasGroupSupport() ? PERMISSIONS.getPrimaryGroup(player) : "default";
    }
}
