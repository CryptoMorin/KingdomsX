package org.kingdoms.outposts;

import com.cryptomorin.xseries.XEntityType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.kingdoms.config.KingdomsConfig;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.data.Pair;
import org.kingdoms.enginehub.EngineHubAddon;
import org.kingdoms.locale.Language;
import org.kingdoms.locale.SupportedLanguage;
import org.kingdoms.locale.messenger.StaticMessenger;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.services.managers.ServiceHandler;
import org.kingdoms.utils.XScoreboard;
import org.kingdoms.utils.bossbars.BossBarSession;
import org.kingdoms.utils.time.TimeFormatter;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class OutpostEvent {
    protected static final Map<String, OutpostEvent> EVENTS = new HashMap<>();
    protected static final Map<UUID, OutpostEvent> KINGDOMS_IN_EVENTS = new HashMap<>();

    private final int level = 1;
    private final @NonNull Outpost outpost;
    private final long time;
    private final @NonNull Map<UUID, OutpostParticipant> participants = new HashMap<>();
    private final @Nullable BossBarSession bossBar;
    private final @NonNull XScoreboard scoreboard;
    private long started;
    private @Nullable BukkitTask task;

    private OutpostEvent(Outpost outpost, long time) {
        this.outpost = Objects.requireNonNull(outpost, "Cannot create outpust event from null outpost");
        this.time = time;

        if (outpost.getBossBarSettings() != null) {
            bossBar = new BossBarSession(outpost.getBossBarSettings());
            bossBar.setVisible(false);
        } else bossBar = null;

        scoreboard = new XScoreboard("main",
                new StaticMessenger(KingdomsConfig.OUTPOST_EVENTS_SCOREBOARD_TITLE.getManager().getString())
                        .getProvider(Language.getDefault()).getMessage(),
                new MessagePlaceholderProvider());
    }

    public static Map<UUID, OutpostEvent> getKingdomsInEvents() {
        return KINGDOMS_IN_EVENTS;
    }

    public static OutpostEvent getJoinedEvent(UUID player) {
        KingdomPlayer kp = KingdomPlayer.getKingdomPlayer(player);
        Kingdom kingdom = kp.getKingdom();

        if (kingdom == null) return null;
        return getJoinedEvent(kingdom);
    }

    public static OutpostEvent getJoinedEvent(Kingdom kingdom) {
        for (OutpostEvent events : OutpostEvent.getEvents().values()) {
            if (events.participants.containsKey(kingdom.getId())) {
                return events;
            }
        }
        return null;
    }

    public static Map<String, OutpostEvent> getEvents() {
        return EVENTS;
    }

    public static OutpostEvent getEvent(String outpost) {
        return EVENTS.get(outpost);
    }

    public static boolean isEventRunning(String outpost) {
        return EVENTS.containsKey(outpost);
    }

    public static OutpostEvent startEvent(Outpost outpost, long time, long startTime) {
        Objects.requireNonNull(outpost, "Outpost cannot be null");
        if (time <= 0) throw new IllegalArgumentException("Outpost event time cannot be less than or equal to 0");
        if (EVENTS.containsKey(outpost.getName()))
            throw new IllegalArgumentException("Event for outpost '" + outpost + "' has already started");

        OutpostEvent event = new OutpostEvent(outpost, time);
        EVENTS.put(outpost.getName(), event);
        event.start(startTime);
        return event;
    }

    public boolean hasStarted() {
        return started != 0;
    }

    public void start(long startTime) {
        task = Bukkit.getScheduler().runTaskLater(Kingdoms.get(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                OutpostsLang.COMMAND_OUTPOST_START_STARTED.sendMessage(player, "outpost", outpost.getName());
            }
            started = System.currentTimeMillis();

            task = new BukkitRunnable() {
                final Objective objective = scoreboard.getScoreboard().getObjective("main");

                @Override
                public void run() {
                    long passed = System.currentTimeMillis() - started;
                    if (passed >= time) {
                        stop(true);
                        return;
                    }

                    if (bossBar != null) {
                        double left = (double) passed / time;
                        bossBar.updateTitle(new MessagePlaceholderProvider().raw("left", TimeFormatter.of(passed - time)));
                        bossBar.setProgress(left);
                    }

                    Iterator<Map.Entry<UUID, OutpostParticipant>> iter = participants.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<UUID, OutpostParticipant> kingdoms = iter.next();
                        Kingdom kingdom = Kingdom.getKingdom(kingdoms.getKey());
                        if (kingdom == null) {
                            iter.remove();
                            continue;
                        }

                        int scored = kingdoms.getValue().getScore();
                        for (Player member : kingdom.getOnlineMembers()) {
                            display(member);
                            if (member.getWorld().getName().equals(outpost.getSpawn().getWorld().getName())) {
                                if (EngineHubAddon.INSTANCE.getWorldGuard().isLocationInRegion(member.getLocation(), outpost.getRegion())) scored++;
                            }
                        }
                        kingdoms.getValue().setScore(scored);

                        if (objective != null) {
                            Score score = objective.getScore(ChatColor.GREEN + kingdom.getName());
                            score.setScore(scored);
                        }
                    }
                }
            }.runTaskTimerAsynchronously(Kingdoms.get(), 0, 1);
            bossBar.setVisible(true);
        }, startTime);
    }

    public boolean isFull() {
        int max = outpost.getMaxParticipants();
        return max > 0 && participants.size() >= max;
    }

    @Nullable
    public Pair<Kingdom, OutpostParticipant> stop(boolean reward) {
        if (bossBar != null) bossBar.removeAll();
        if (task != null) task.cancel();
        EVENTS.remove(outpost.getName());
        if (participants.isEmpty()) return null; // The event ended before anyone could join.
        participants.keySet().forEach(KINGDOMS_IN_EVENTS::remove);

        // Find the winner.
        Map.Entry<UUID, OutpostParticipant> winner = null;
        for (Map.Entry<UUID, OutpostParticipant> participant : participants.entrySet()) {
            long score = participant.getValue().getScore();
            if (winner == null || winner.getValue().getScore() < score) winner = participant;
        }

        // Notify other participants of the results and tp out.
        for (Map.Entry<UUID, OutpostParticipant> participant : participants.entrySet()) {
            boolean lost = !participant.getKey().equals(winner.getKey());
            Kingdom kingdom = Kingdom.getKingdom(participant.getKey());
            for (Player player : kingdom.getOnlineMembers()) {
                if (lost) {
                    OutpostsLang.COMMAND_OUTPOST_JOIN_LOST.sendMessage(player, "outpost", outpost.getName());
                    if (EngineHubAddon.INSTANCE.getWorldGuard().isLocationInRegion(player.getLocation(), outpost.getRegion())) {
                        Bukkit.getScheduler().runTask(Kingdoms.get(),
                                () -> player.teleport(outpost.getSpawn()));
                    }
                } else
                    OutpostsLang.COMMAND_OUTPOST_JOIN_WIN.sendMessage(player, "outpost", outpost.getName(), "resource-points", outpost.getRewards().getResourcePoints(1),
                            "money", outpost.getRewards().getMoney(1));
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }

        // Reward animation with fireworks
        new BukkitRunnable() {
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            final int times = random.nextInt(10, 20);
            final FireworkEffect.Type[] types = FireworkEffect.Type.values();
            int i = 0;

            @Override
            public void run() {
                Location loc = outpost.getCenter().clone().add(random.nextDouble(-5, 5), random.nextDouble(-1, 3), random.nextDouble(-5, 5));
                Firework firework = (Firework) loc.getWorld().spawnEntity(loc, XEntityType.FIREWORK_ROCKET.get());
                FireworkMeta meta = firework.getFireworkMeta();

                meta.setPower(0);
                int max = random.nextInt(3, 10);
                List<FireworkEffect> effects = new ArrayList<>(max);
                for (int j = 0; j < max; j++) {
                    FireworkEffect.Builder builder = FireworkEffect.builder();
                    if (random.nextInt(0, 2) == 1) builder.flicker(true);
                    if (random.nextInt(0, 2) == 1) builder.trail(true);
                    builder.with(types[random.nextInt(0, types.length)]);

                    List<org.bukkit.Color> colors = new ArrayList<>();
                    int colorMax = random.nextInt(1, 4);
                    for (int k = 0; k < colorMax; k++)
                        colors.add(org.bukkit.Color.fromRGB(random.nextInt(1, 255), random.nextInt(1, 255), random.nextInt(1, 255)));
                    builder.withColor(colors);

                    List<org.bukkit.Color> fades = new ArrayList<>();
                    int fadeMax = random.nextInt(1, 4);
                    for (int k = 0; k < fadeMax; k++)
                        fades.add(org.bukkit.Color.fromRGB(random.nextInt(1, 255), random.nextInt(1, 255), random.nextInt(1, 255)));
                    builder.withFade(fades);

                    effects.add(builder.build());
                }

                meta.addEffects(effects);
                firework.setFireworkMeta(meta);
                if (i++ == times) cancel();
            }
        }.runTaskTimer(Kingdoms.get(), 1, 10);

        // Giving the actual rewards to the winner
        Kingdom kingdom = Kingdom.getKingdom(winner.getKey());
        if (reward) {
            OutpostRewards rewards = outpost.getRewards();
            kingdom.getResourcePoints().add(rewards.getResourcePoints(level));
            kingdom.getBank().add(rewards.getMoney(level));
            rewards.performCommands(kingdom);
            List<ItemStack> items = rewards.getItems();

            if (items != null && !items.isEmpty()) {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                new BukkitRunnable() {
                    int i = 0;

                    @Override
                    public void run() {
                        Location loc = outpost.getCenter().clone().add(random.nextDouble(-5, 5), random.nextDouble(-1, 3), random.nextDouble(-5, 5));
                        loc.getWorld().dropItemNaturally(loc, items.get(i));
                        if (++i == items.size()) cancel();
                    }
                }.runTaskTimer(Kingdoms.get(), 1, 10);
            }
        }

        return Pair.of(kingdom, winner.getValue());
    }

    @NonNull
    public OutpostParticipant participate(Player requester, Kingdom kingdom) {
        OutpostParticipant participant = new OutpostParticipant(requester.getUniqueId());
        participants.put(kingdom.getId(), participant);
        KINGDOMS_IN_EVENTS.put(kingdom.getId(), this);

        Score score = scoreboard.getScoreboard().getObjective("main").getScore(ChatColor.GREEN + kingdom.getName());
        score.setScore(0);

        return participant;
    }

    public void display(Player player) {
        bossBar.addPlayer(player);
        scoreboard.setForPlayer(player);
    }

    public long getTime() {
        return time;
    }

    public @NonNull Outpost getOutpost() {
        return outpost;
    }

    public @NonNull Map<UUID, OutpostParticipant> getParticipants() {
        return participants;
    }

    public @NonNull XScoreboard getScoreboard() {
        return scoreboard;
    }
}
