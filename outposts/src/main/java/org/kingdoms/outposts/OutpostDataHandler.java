package org.kingdoms.outposts;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.main.locale.MessageHandler;
import org.kingdoms.utils.LocationUtils;
import org.kingdoms.utils.bossbars.BossBarSettings;
import org.kingdoms.utils.compilers.MathCompiler;
import org.kingdoms.utils.config.ConfigSection;
import org.kingdoms.utils.config.adapters.YamlFile;
import org.kingdoms.utils.xseries.XItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class OutpostDataHandler {
    protected static final YamlFile DATA = new YamlFile(new File(Kingdoms.getFolder().toFile(), "outposts.yml")).load();

    public static void saveOutposts() {
        ConfigSection config = DATA.getConfig();

        for (Outpost outpost : Outpost.getOutposts().values()) {
            ConfigSection section = config.createSection(outpost.getName());
            section.set("region", outpost.getRegion());
            section.set("spawn", LocationUtils.toString(outpost.getSpawn()));
            section.set("center", LocationUtils.toString(outpost.getCenter()));
            section.set("cost", outpost.getMoneyCost());
            section.set("resource-points-cost", outpost.getResourcePointsCost());
            section.set("max-participants", outpost.getMaxParticipants());
            section.set("min-online-members", outpost.getMinOnlineMembers());
            BossBarSettings bossbar = outpost.getBossBarSettings();
            if (bossbar != null) bossbar.toString(section.createSection("bossbar"));

            ConfigSection rewardsSection = section.createSection("rewards");
            OutpostRewards rewards = outpost.getRewards();
            rewardsSection.set("resource-points", rewards.getResourcePoints());
            rewardsSection.set("money", rewards.getMoney());

            if (!rewards.getCommands().isEmpty()) {
                rewardsSection.set("commands", rewards.getCommands());
            }

            if (!rewards.getItems().isEmpty()) {
                ConfigSection items = rewardsSection.createSection("items");
                int i = 0;
                for (ItemStack item : rewards.getItems()) {
                    ConfigSection itemEntry = items.createSection(Integer.toString(i));
                    XItemStack.serialize(item, itemEntry.toBukkitConfigurationSection());
                    i++;
                }
            }
        }
        DATA.saveConfig();
    }

    public static void loadOutposts() {
        OutpostAddon.get().getLogger().info("Loading outposts...");

        Outpost.getOutposts().clear();
        ConfigSection config = DATA.getConfig();
        if (config == null) return;

        for (String name : config.getKeys()) {
            ConfigSection section = config.getSection(name);
            ConfigSection rewardSection = section.getSection("rewards");
            ConfigSection itemsSection = rewardSection.getSection("items");
            List<ItemStack> itemRewards = new ArrayList<>();

            if (itemsSection != null) {
                for (String item : itemsSection.getKeys()) {
                    ItemStack itemStack = XItemStack.deserialize(itemsSection.getSection(item).toBukkitConfigurationSection(),
                            MessageHandler::colorize);
                    itemRewards.add(itemStack);
                }
            }

            ConfigSection bossbarSection = section.getSection("bossbar");
            BossBarSettings bossbar = bossbarSection == null ? null : BossBarSettings.fromSection(bossbarSection);

            List<String> commands = rewardSection.getStringList("commands");

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
