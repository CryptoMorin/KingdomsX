package org.kingdoms.outposts;

import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.utils.MathUtils;
import org.kingdoms.utils.string.StringUtils;

import java.util.List;
import java.util.Map;

public class OutpostRewards {
    private final String resourcePoints;
    private final String money;
    private final List<String> commands;
    private final Map<Integer, List<ItemStack>> items;

    public OutpostRewards(String resourcePoints, String money, List<String> commands, Map<Integer, List<ItemStack>> items) {
        this.resourcePoints = resourcePoints;
        this.money = money;
        this.commands = commands;
        this.items = items;
    }

    public int getResourcePoints(int lvl) {
        return (int) MathUtils.evaluateEquation(resourcePoints, "lvl", lvl);
    }

    public void performCommands(Kingdom kingdom) {
        for (OfflinePlayer member : kingdom.getPlayerMembers()) {
            StringUtils.performCommands(member, commands);
        }
    }

    public double getMoney(int lvl) {
        return MathUtils.evaluateEquation(money, "lvl", lvl);
    }

    public List<ItemStack> getItems(int lvl) {
        int closest = 1;
        for (int level : items.keySet()) {
            if (level == lvl) {
                closest = level;
                break;
            }

            if (level < lvl && level > closest) closest = level;
        }

        return items.get(closest);
    }

    public Map<Integer, List<ItemStack>> getItems() {
        return items;
    }

    public String getMoney() {
        return money;
    }

    public String getResourcePoints() {
        return resourcePoints;
    }
}