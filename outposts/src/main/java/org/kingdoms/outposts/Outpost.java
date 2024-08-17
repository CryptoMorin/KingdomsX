package org.kingdoms.outposts;

import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.kingdoms.utils.bossbars.BossBarSettings;
import org.kingdoms.utils.compilers.MathCompiler;
import org.kingdoms.utils.compilers.expressions.MathExpression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Outpost {
    private static final Map<String, Outpost> OUTPOSTS = new HashMap<>();

    private @NonNull String name, region;
    private MathExpression resourcePointsCost, moneyCost;
    private int maxParticipants, minOnlineMembers;
    private @NonNull OutpostRewards rewards;
    private @NonNull Location spawn, center;
    private @Nullable BossBarSettings bossBarSettings;

    public Outpost(@NonNull String name, @NonNull String region, @NonNull Location spawn, @NonNull Location center) {
        this.name = name;
        this.region = region;
        this.spawn = spawn;
        this.center = center;
        this.rewards = new OutpostRewards("3000", "1000", new ArrayList<>(), new ArrayList<>());
        setDefaultBossBarSettings();
    }

    public Outpost(@NonNull String name, @NonNull String region, @NonNull Location spawn, @NonNull Location center,
                   MathExpression cost, MathExpression resourcePointsCost,
                   int maxParticipants, int minOnlineMembers,
                   @Nullable BossBarSettings bossBarSettings, @NonNull OutpostRewards rewards) {
        this.name = name;
        this.region = region;
        this.moneyCost = cost;
        this.spawn = spawn;
        this.center = center;
        this.resourcePointsCost = resourcePointsCost;
        this.bossBarSettings = bossBarSettings;
        this.maxParticipants = maxParticipants;
        this.minOnlineMembers = minOnlineMembers;
        this.rewards = rewards;
    }

    public static void registerOutpost(Outpost outpost) {
        OUTPOSTS.put(outpost.name, outpost);
    }

    public static Outpost getOutpost(String name) {
        return OUTPOSTS.get(name);
    }

    public static Map<String, Outpost> getOutposts() {
        return OUTPOSTS;
    }

    public OutpostEvent getEvent() {
        return OutpostEvent.EVENTS.get(this.name);
    }

    public int getMinOnlineMembers() {
        return minOnlineMembers;
    }

    public void setMinOnlineMembers(int minOnlineMembers) {
        this.minOnlineMembers = minOnlineMembers;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public @NonNull String getRegion() {
        return region;
    }

    public void setRegion(@NonNull String region) {
        this.region = region;
    }

    public @NonNull String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public @NonNull OutpostRewards getRewards() {
        return rewards;
    }

    public void setRewards(@NonNull OutpostRewards rewards) {
        this.rewards = rewards;
    }

    public @NonNull Location getSpawn() {
        return spawn;
    }

    public void setSpawn(@NonNull Location spawn) {
        this.spawn = spawn;
    }

    public @NonNull Location getCenter() {
        return center;
    }

    public void setCenter(@NonNull Location center) {
        this.center = center;
    }

    public @Nullable BossBarSettings getBossBarSettings() {
        return bossBarSettings;
    }

    public void setDefaultBossBarSettings() {
        this.bossBarSettings = new BossBarSettings("&8-=[ &6" + name + " &2Outpost Event &8]=-", BarColor.BLUE, BarStyle.SEGMENTED_6);
    }

    public void setBossBarSettings(@Nullable BossBarSettings bossBarSettings) {
        this.bossBarSettings = bossBarSettings;
    }

    public MathExpression getResourcePointsCost() {
        return resourcePointsCost;
    }

    public void setResourcePointsCost(MathExpression resourcePointsCost) {
        this.resourcePointsCost = resourcePointsCost;
    }

    public MathExpression getMoneyCost() {
        return moneyCost;
    }

    public void setMoneyCost(MathExpression moneyCost) {
        this.moneyCost = moneyCost;
    }

    @Override
    public String toString() {
        return "Outpost{" +
                "name='" + name + '\'' +
                '}';
    }
}
