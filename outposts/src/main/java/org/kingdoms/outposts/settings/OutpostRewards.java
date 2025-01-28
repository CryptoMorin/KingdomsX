package org.kingdoms.outposts.settings;

import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.utils.MathUtils;
import org.kingdoms.utils.string.Strings;

import java.util.List;

public class OutpostRewards {
    private String resourcePoints;
    private String money;
    private List<String> commands;
    private List<ItemStack> items;

    public OutpostRewards(String resourcePoints, String money, List<String> commands, List<ItemStack> items) {
        this.resourcePoints = resourcePoints;
        this.money = money;
        this.commands = commands;
        this.items = items;
    }

    public int getResourcePoints(int lvl) {
        return (int) MathUtils.evaluateEquation(resourcePoints, "lvl", lvl);
    }

    public String getResourcePoints() {
        return resourcePoints;
    }

    public double getMoney(int lvl) {
        return MathUtils.evaluateEquation(money, "lvl", lvl);
    }

    public String getMoney() {
        return money;
    }

    public void performCommands(Kingdom kingdom) {
        for (OfflinePlayer member : kingdom.getPlayerMembers()) {
            Strings.performCommands(member, commands);
        }
    }

    public List<ItemStack> getItems() {
        return items;
    }


    public void setItems(List<ItemStack> items) {
        this.items = items;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setMoney(String money) {
        this.money = money;
    }

    public void setResourcePoints(String resourcePoints) {
        this.resourcePoints = resourcePoints;
    }

}