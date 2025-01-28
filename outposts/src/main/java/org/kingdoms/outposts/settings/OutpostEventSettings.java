package org.kingdoms.outposts.settings;

import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.kingdoms.outposts.OutpostEvent;
import org.kingdoms.utils.bossbars.BossBarSettings;
import org.kingdoms.utils.compilers.expressions.MathExpression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutpostEventSettings {
    private static final Map<String, OutpostEventSettings> OUTPOSTS = new HashMap<>();

    private @NonNull String name, region;
    private MathExpression resourcePointsCost, moneyCost;
    private int maxParticipants, minOnlineMembers;
    private List<OutpostArenaMob> arenaMobs;
    private @NonNull OutpostRewards rewards;
    private @NonNull Location spawn, center;
    private @Nullable BossBarSettings bossBarSettings;

    public OutpostEventSettings(@NonNull String name, @NonNull String region, @NonNull Location spawn, @NonNull Location center) {
        this.name = name;
        this.region = region;
        this.spawn = spawn;
        this.center = center;
        this.rewards = new OutpostRewards("3000", "1000", new ArrayList<>(), new ArrayList<>());
        this.arenaMobs = new ArrayList<>();
        setDefaultBossBarSettings();
    }

    public OutpostEventSettings(@NonNull String name, @NonNull String region,
                                @NonNull Location spawn, @NonNull Location center,
                                MathExpression cost, MathExpression resourcePointsCost,
                                int maxParticipants, int minOnlineMembers,
                                @NonNull  List<OutpostArenaMob> arenaMobs,
                                @Nullable BossBarSettings bossBarSettings,
                                @NonNull OutpostRewards rewards) {
        this.name = name;
        this.region = region;
        this.moneyCost = cost;
        this.spawn = spawn;
        this.center = center;
        this.resourcePointsCost = resourcePointsCost;
        this.bossBarSettings = bossBarSettings;
        this.maxParticipants = maxParticipants;
        this.minOnlineMembers = minOnlineMembers;
        this.arenaMobs = arenaMobs;
        this.rewards = rewards;
    }

    public static void registerOutpost(OutpostEventSettings outpost) {
        OUTPOSTS.put(outpost.name, outpost);
    }

    public static OutpostEventSettings getOutpost(String name) {
        return OUTPOSTS.get(name);
    }

    public static Map<String, OutpostEventSettings> getOutposts() {
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

    public List<OutpostArenaMob> getArenaMobs() {
        return arenaMobs;
    }

    public void setArenaMobs(List<OutpostArenaMob> arenaMobs) {
        this.arenaMobs = arenaMobs;
    }

    public void setDefaultBossBarSettings() {
        this.bossBarSettings = new BossBarSettings("{$sep}-=[ &6" + name + " &2Outpost Event {$sep}]=-", BarColor.BLUE, BarStyle.SEGMENTED_6);
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
        return getClass().getSimpleName() + '(' + name + ')';
    }
}
