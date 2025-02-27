package org.kingdoms.outposts;

import com.cryptomorin.xseries.XItemStack;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.outposts.settings.OutpostArenaMob;
import org.kingdoms.outposts.settings.OutpostEventSettings;
import org.kingdoms.outposts.settings.OutpostRewards;
import org.kingdoms.utils.KingdomsItemDeserializer;
import org.kingdoms.utils.LocationUtils;
import org.kingdoms.utils.bossbars.BossBarSettings;
import org.kingdoms.utils.compilers.MathCompiler;
import org.kingdoms.utils.compilers.expressions.MathExpression;
import org.kingdoms.utils.config.ConfigSection;
import org.kingdoms.utils.config.adapters.YamlFile;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class OutpostDataHandler {
    protected static final YamlFile DATA = new YamlFile(new File(Kingdoms.getFolder().toFile(), "outposts.yml")).load();

    public static void saveOutposts() {
        ConfigSection config = DATA.getConfig();

        for (OutpostEventSettings outpost : OutpostEventSettings.getOutposts().values()) {
            ConfigSection section = config.createSection(outpost.getName());
            section.set("region", outpost.getRegion());
            section.set("spawn", LocationUtils.toString(outpost.getSpawn()));
            section.set("center", LocationUtils.toString(outpost.getCenter()));
            if (outpost.getMoneyCost() != null) section.set("cost", outpost.getMoneyCost().getOriginalString());
            if (outpost.getResourcePointsCost() != null)
                section.set("resource-points-cost", outpost.getResourcePointsCost().getOriginalString());
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

            int num = 0;
            ConfigSection arenaMobsSection = section.createSection("arena-mobs");
            for (OutpostArenaMob arenaMob : outpost.getArenaMobs()) {
                ConfigSection arenaMobSection = arenaMobsSection.createSection(String.valueOf(num++));

                if (arenaMob.getLabel() != null) arenaMobSection.set("label", arenaMob.getLabel());
                if (arenaMob.getMaxSpawnCount() != 0)
                    arenaMobSection.set("max-spawn-count", arenaMob.getMaxSpawnCount());
                if (arenaMob.getSpawnInterval() != null)
                    arenaMobSection.set("spawn-interval", arenaMob.getSpawnInterval().getSeconds() + "s");
                if (arenaMob.getSpawnLocation() != null)
                    arenaMobSection.set("spawn-location", LocationUtils.toString(arenaMob.getSpawnLocation()));
                if (arenaMob.getDamageBonus() != null)
                    arenaMobSection.set("damage-bonus", arenaMob.getDamageBonus().getOriginalString());
                if (arenaMob.getEntitySettings() != null) arenaMobSection.set("entity", arenaMob.getEntitySettings());
            }
        }
        DATA.saveConfig();
    }

    public static void loadOutposts() {
        OutpostAddon.get().getLogger().info("Loading outposts...");

        OutpostEventSettings.getOutposts().clear();
        ConfigSection config = DATA.getConfig();
        if (config == null) return;

        for (String name : config.getKeys()) {
            ConfigSection section = config.getSection(name);
            ConfigSection rewardSection = section.getSection("rewards");
            ConfigSection itemsSection = rewardSection.getSection("items");
            List<ItemStack> itemRewards = new ArrayList<>();

            if (itemsSection != null) {
                for (String itemName : itemsSection.getKeys()) {
                    ItemStack rewardItem = new KingdomsItemDeserializer()
                            .withSection(itemsSection.getSection(itemName))
                            .deserialize();
                    itemRewards.add(rewardItem);
                }
            }

            ConfigSection bossbarSection = section.getSection("bossbar");
            BossBarSettings bossbar = bossbarSection == null ? null : BossBarSettings.fromSection(bossbarSection);

            List<String> commands = rewardSection.getStringList("commands");

            Location spawn = LocationUtils.fromString(section.getString("spawn"));
            Location center = LocationUtils.fromString(section.getString("center"));
            OutpostRewards rewards = new OutpostRewards(rewardSection.getString("resource-points"), rewardSection.getString("money"), commands, itemRewards);

            MathCompiler.Expression cost = MathCompiler.compile(section.getString("cost"));
            MathCompiler.Expression rp = MathCompiler.compile(section.getString("resource-points-cost"));

            List<OutpostArenaMob> arenaMobs = new ArrayList<>();
            ConfigSection arenaMobsSection = section.getSection("arena-mobs");
            if (arenaMobsSection != null) {
                for (ConfigSection arenaMobSection : arenaMobsSection.getSections().values()) {
                    String label = arenaMobSection.getString("label");
                    int maxSpawnCount = arenaMobSection.getInt("max-spawn-count");
                    Duration spawnInterval = arenaMobSection.getTime("spawn-interval");

                    String mobLoc = arenaMobSection.getString("spawn-location");
                    Location spawnLocation = mobLoc == null ? null : LocationUtils.fromString(mobLoc);

                    MathExpression damageBonus = MathCompiler.compile(arenaMobSection.getString("damage-bonus")).nullIfDefault();
                    ConfigSection entity = arenaMobSection.getSection("entity");
                    arenaMobs.add(new OutpostArenaMob(label, entity, maxSpawnCount, spawnInterval, spawnLocation, damageBonus));
                }
            }

            OutpostEventSettings outpost = new OutpostEventSettings(name, section.getString("region"), spawn, center,
                    cost.isDefault() ? null : cost, rp.isDefault() ? null : rp,
                    section.getInt("max-participants"), section.getInt("min-online-members"),
                    arenaMobs,
                    bossbar, rewards);

            OutpostEventSettings.getOutposts().put(name, outpost);
        }
    }
}
