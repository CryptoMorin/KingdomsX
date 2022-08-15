package org.kingdoms.outposts;

import com.cryptomorin.xseries.XItemStack;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.main.locale.MessageHandler;
import org.kingdoms.utils.LocationUtils;
import org.kingdoms.utils.bossbars.BossBarSettings;
import org.kingdoms.utils.compilers.MathCompiler;
import org.kingdoms.utils.config.ConfigSection;
import org.kingdoms.utils.config.adapters.YamlFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OutpostHandler {
    protected static final YamlFile DATA = new YamlFile(new File(Kingdoms.get().getDataFolder(), "outposts.yml"));

    public static void saveOutposts() {
        ConfigSection config = DATA.getConfig();
        for (Outpost outpost : Outpost.getOutposts().values()) {
            ConfigSection section = config.createSection(outpost.getName());
            section.set("region", outpost.getRegion());
            section.set("spawn", LocationUtils.toString(outpost.getSpawn()));
            section.set("center", LocationUtils.toString(outpost.getCenter()));
            section.set("cost", outpost.getCost());
            section.set("resource-points-cost", outpost.getResourcePointsCost());
            section.set("max-participants", outpost.getMaxParticipants());
            section.set("min-online-members", outpost.getMinOnlineMembers());
            BossBarSettings bossbar = outpost.getBossBarSettings();
            if (bossbar != null) bossbar.toString(section.createSection("bossbar"));

            ConfigSection rewardsSection = section.createSection("rewards");
            OutpostRewards rewards = outpost.getRewards();
            rewardsSection.set("resource-points", rewards.getResourcePoints());
            rewardsSection.set("money", rewards.getMoney());

            ConfigSection items = rewardsSection.createSection("items");
            for (Map.Entry<Integer, List<ItemStack>> item : rewards.getItems().entrySet()) {
                ConfigSection levelSection = items.createSection(Integer.toString(item.getKey()));

                int i = 0;
                for (ItemStack itemstack : item.getValue()) {
                    ConfigSection itemEntry = levelSection.createSection(Integer.toString(i));
                    XItemStack.serialize(itemstack, itemEntry.toBukkitConfigurationSection());
                    i++;
                }
            }
        }
        DATA.saveConfig();
    }

    public static void loadOutposts() {
        DATA.load();
        ConfigSection config = DATA.getConfig();
        if (config == null) return;

        for (String name : config.getKeys()) {
            ConfigSection section = config.getSection(name);
            ConfigSection rewardSection = section.getSection("rewards");
            ConfigSection itemsSection = rewardSection.getSection("items");
            Map<Integer, List<ItemStack>> itemRewards = new HashMap<>();

            for (String level : itemsSection.getKeys()) {
                int lvl = Integer.parseInt(level);
                List<ItemStack> items = new ArrayList<>();
                ConfigSection itemSection = itemsSection.getSection(level);

                for (String item : itemSection.getKeys()) {
                    ItemStack itemStack = XItemStack.deserialize(itemSection.getSection(item).toBukkitConfigurationSection(),
                            MessageHandler::colorize);
                    items.add(itemStack);
                }
                itemRewards.put(lvl, items);
            }

            ConfigSection bossbarSection = section.getSection("bossbar");
            BossBarSettings bossbar = bossbarSection == null ? null : BossBarSettings.fromSection(bossbarSection);

            List<String> commands = config.getStringList("commands");

            Location spawn = LocationUtils.fromString(section.getString("spawn"));
            Location center = LocationUtils.fromString(section.getString("center"));
            OutpostRewards rewards = new OutpostRewards(rewardSection.getString("resource-points"), rewardSection.getString("money"), commands, itemRewards);

            Outpost outpost = new Outpost(name, section.getString("region"), spawn, center,
                    MathCompiler.compile(section.getString("cost")), MathCompiler.compile(section.getString("resource-points-cost")),
                    section.getInt("max-participants"), section.getInt("min-online-members"), bossbar, rewards);
            Outpost.getOutposts().put(name, outpost);
        }
    }
}
