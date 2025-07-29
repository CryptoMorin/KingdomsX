/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package org.kingdoms.platform.folia;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.scheduler.*;
import org.kingdoms.scheduler.convert.InjectedConsumerRunnable;
import org.kingdoms.scheduler.convert.UnconsumedRunnable;
import org.kingdoms.utils.time.internal.TickTemporalUnit;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public abstract class FoliaSchedulerFactory {
    public static final GlobalRegionScheduler GLOBAL_SCHEDULER = Bukkit.getServer().getGlobalRegionScheduler();
    private static final RegionScheduler REGION_SCHEDULER = Bukkit.getServer().getRegionScheduler();

    private final GlobalRegionScheduler globalScheduler;
    private final Plugin plugin;
    private final FoliaGlobalTaskScheduler taskScheduler;
    private final Executor globalSchedulerExecutor;

    public FoliaSchedulerFactory(Plugin loader, Executor globalSchedulerExecutor) {
        this.plugin = loader;
        this.globalSchedulerExecutor = globalSchedulerExecutor;
        this.globalScheduler = GLOBAL_SCHEDULER;
        this.taskScheduler = new FoliaGlobalTaskScheduler();
    }

    @SuppressWarnings("ReturnOfInnerClass")
    public TaskScheduler getGlobalTaskScheduler() {
        return taskScheduler;
    }

    protected abstract <T extends Task> Consumer<T> trace(Consumer<T> consumer, int skippedTraces);

    protected abstract Runnable trace(Runnable runnable, int skippedTraces);

    private <T extends Task> Consumer<ScheduledTask> traceFolia(Consumer<T> consumer, T task) {
        Consumer<T> traced = trace(consumer, 1);
        return x -> traced.accept(task);
    }

    public static void runGlobalUntraced(Plugin plugin, Runnable operation) {
        GLOBAL_SCHEDULER.run(plugin, task -> operation.run());
    }

    public void runGlobal(Runnable runnable) {
        UnconsumedRunnable<Task> consumer = new UnconsumedRunnable<>(runnable);
        FoliaTask abstractTask = new FoliaTask(consumer, Task.ExecutionContextType.SYNC);
        ScheduledTask foliaTask = FoliaSchedulerFactory.GLOBAL_SCHEDULER.run(plugin, traceFolia(consumer, abstractTask));
        abstractTask.setFoliaTask(foliaTask);
    }

    private static boolean successfullyCancelled(ScheduledTask.CancelledState state) {
        return state != ScheduledTask.CancelledState.RUNNING && state != ScheduledTask.CancelledState.ALREADY_EXECUTED;
    }

    private interface FoliaTaskContainer {
        void setFoliaTask(ScheduledTask task);
    }

    public abstract class AbstractFoliaTaskScheduler implements TaskScheduler {
        @NotNull
        @Override
        public final Task.ExecutionContextType getExecutionContextType() {
            return Task.ExecutionContextType.SYNC;
        }

        public final @NotNull <T extends Task> Consumer<T> unconsumableRunnable(@NotNull Runnable runnable) {
            return new UnconsumedRunnable<>(trace(runnable, 1));
        }

        // TODO Use this to switch to InstantOrDelayedTaskScheduler when possible.
        public abstract boolean isOwned();

        @NotNull
        @Override
        public Executor getExecutor() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FoliaTask extends AbstractTask implements FoliaTaskContainer {
        private ScheduledTask task;

        private FoliaTask(Consumer<Task> runnable, ExecutionContextType executionContextType) {
            super(executionContextType, runnable);
        }

        @Override
        public void setFoliaTask(ScheduledTask task) {
            this.task = task;
        }

        public boolean cancel() {
            return successfullyCancelled(task.cancel());
        }
    }

    private static final class FoliaDelayedTask extends AbstractDelayedTask implements FoliaTaskContainer {
        private ScheduledTask task;

        private FoliaDelayedTask(Consumer<DelayedTask> runnable, Duration delay, ExecutionContextType executionContextType) {
            super(runnable, delay, executionContextType);
        }

        @Override
        public boolean cancel() {
            super.cancel();
            return successfullyCancelled(task.cancel());
        }

        @Override
        public void setFoliaTask(ScheduledTask task) {
            this.task = task;
        }
    }

    private static final class FoliaDelayedRepeatingTask extends AbstractDelayedRepeatingTask implements FoliaTaskContainer {
        private ScheduledTask task;

        private FoliaDelayedRepeatingTask(Consumer<DelayedRepeatingTask> runnable, Duration initialDelay, Duration intervalDelays, ExecutionContextType executionContextType) {
            super(runnable, initialDelay, intervalDelays, executionContextType);
        }

        @Override
        public boolean cancel() {
            super.cancel();
            return successfullyCancelled(task.cancel());
        }

        @Override
        public void setFoliaTask(ScheduledTask task) {
            this.task = task;
        }
    }

    private final class FoliaGlobalTaskScheduler extends AbstractFoliaTaskScheduler {
        @NotNull
        @Override
        public Executor getExecutor() {
            return globalSchedulerExecutor;
        }

        @Override
        public boolean isOwned() {
            return Bukkit.getServer().isGlobalTickThread();
        }

        @NotNull
        @Override
        public Task execute(@NotNull Consumer<Task> runnable) {
            AbstractTask task = new AbstractTask(getExecutionContextType(), runnable);
            globalSchedulerExecutor.execute(new InjectedConsumerRunnable<>(runnable, task));
            return task;
        }

        @NotNull
        @Override
        public DelayedTask delayed(@NotNull Duration delay, @NotNull Consumer<DelayedTask> runnable) {
            FoliaDelayedTask abstractTask = new FoliaDelayedTask(runnable, delay, getExecutionContextType());
            ScheduledTask foliaTask = globalScheduler.runDelayed(plugin, traceFolia(runnable, abstractTask), TickTemporalUnit.toTicks(delay));
            abstractTask.setFoliaTask(foliaTask);
            return abstractTask;
        }

        @NotNull
        @Override
        public DelayedRepeatingTask repeating(@NotNull Duration initialDelay, @NotNull Duration intervalDelays, @NotNull Consumer<DelayedRepeatingTask> runnable) {
            FoliaDelayedRepeatingTask abstractTask = new FoliaDelayedRepeatingTask(runnable, initialDelay, intervalDelays, getExecutionContextType());
            ScheduledTask foliaTask = globalScheduler.runAtFixedRate(plugin, traceFolia(runnable, abstractTask), normalizeTicks(initialDelay), TickTemporalUnit.toTicks(intervalDelays));
            abstractTask.setFoliaTask(foliaTask);
            return abstractTask;
        }
    }

    private static long normalizeTicks(Duration duration) {
        if (duration.isZero()) return 1;
        return TickTemporalUnit.toTicks(duration);
    }

    public final class FoliaEntityTaskScheduler extends AbstractFoliaTaskScheduler {
        private final Entity entity;
        private final EntityScheduler scheduler;

        public FoliaEntityTaskScheduler(Entity entity) {
            this.entity = entity;
            this.scheduler = entity.getScheduler();
        }

        @Override
        public boolean isOwned() {
            return Bukkit.getServer().isOwnedByCurrentRegion(entity);
        }

        @NotNull
        @Override
        public Task execute(@NotNull Consumer<Task> runnable) {
            FoliaTask abstractTask = new FoliaTask(runnable, getExecutionContextType());
            ScheduledTask foliaTask = scheduler.run(plugin, traceFolia(runnable, abstractTask), null);
            abstractTask.setFoliaTask(foliaTask);
            return abstractTask;
        }

        @NotNull
        @Override
        public DelayedTask delayed(@NotNull Duration delay, @NotNull Consumer<DelayedTask> runnable) {
            FoliaDelayedTask abstractTask = new FoliaDelayedTask(runnable, delay, getExecutionContextType());
            ScheduledTask foliaTask = scheduler.runDelayed(plugin, traceFolia(runnable, abstractTask), null, TickTemporalUnit.toTicks(delay));
            abstractTask.setFoliaTask(foliaTask);
            return abstractTask;
        }

        @NotNull
        @Override
        public DelayedRepeatingTask repeating(@NotNull Duration initialDelay, @NotNull Duration intervalDelays, @NotNull Consumer<DelayedRepeatingTask> runnable) {
            FoliaDelayedRepeatingTask abstractTask = new FoliaDelayedRepeatingTask(runnable, initialDelay, intervalDelays, getExecutionContextType());
            ScheduledTask foliaTask = scheduler.runAtFixedRate(plugin, traceFolia(runnable, abstractTask), null, normalizeTicks(initialDelay), TickTemporalUnit.toTicks(intervalDelays));
            abstractTask.setFoliaTask(foliaTask);
            return abstractTask;
        }
    }

    public final class FoliaRegionalLocationTaskScheduler extends AbstractFoliaTaskScheduler {
        private final Location location;

        public FoliaRegionalLocationTaskScheduler(Location location) {this.location = location;}

        @Override
        public boolean isOwned() {
            return Bukkit.getServer().isOwnedByCurrentRegion(location);
        }

        @NotNull
        @Override
        public Task execute(@NotNull Consumer<Task> runnable) {
            FoliaTask abstractTask = new FoliaTask(runnable, getExecutionContextType());
            ScheduledTask foliaTask = REGION_SCHEDULER.run(plugin, location, traceFolia(runnable, abstractTask));
            abstractTask.setFoliaTask(foliaTask);
            return abstractTask;
        }

        @NotNull
        @Override
        public DelayedTask delayed(@NotNull Duration delay, @NotNull Consumer<DelayedTask> runnable) {
            FoliaDelayedTask abstractTask = new FoliaDelayedTask(runnable, delay, getExecutionContextType());
            ScheduledTask foliaTask = REGION_SCHEDULER.runDelayed(plugin, location, traceFolia(runnable, abstractTask), TickTemporalUnit.toTicks(delay));
            abstractTask.setFoliaTask(foliaTask);
            return abstractTask;
        }

        @NotNull
        @Override
        public DelayedRepeatingTask repeating(@NotNull Duration initialDelay, @NotNull Duration intervalDelays, @NotNull Consumer<DelayedRepeatingTask> runnable) {
            FoliaDelayedRepeatingTask abstractTask = new FoliaDelayedRepeatingTask(runnable, initialDelay, intervalDelays, getExecutionContextType());
            ScheduledTask foliaTask = REGION_SCHEDULER.runAtFixedRate(plugin, location, traceFolia(runnable, abstractTask), normalizeTicks(initialDelay), TickTemporalUnit.toTicks(intervalDelays));
            abstractTask.setFoliaTask(foliaTask);
            return abstractTask;
        }
    }

    public final class FoliaRegionalChunkTaskScheduler extends AbstractFoliaTaskScheduler {
        private final World world;
        private final int chunkX, chunkZ;

        public FoliaRegionalChunkTaskScheduler(World world, int chunkX, int chunkZ) {
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public boolean isOwned() {
            return Bukkit.getServer().isOwnedByCurrentRegion(world, chunkX, chunkZ);
        }

        @NotNull
        @Override
        public Task execute(@NotNull Consumer<Task> runnable) {
            FoliaTask abstractTask = new FoliaTask(runnable, getExecutionContextType());
            ScheduledTask foliaTask = REGION_SCHEDULER.run(plugin, world, chunkX, chunkZ, traceFolia(runnable, abstractTask));
            abstractTask.setFoliaTask(foliaTask);
            return abstractTask;
        }

        @NotNull
        @Override
        public DelayedTask delayed(@NotNull Duration delay, @NotNull Consumer<DelayedTask> runnable) {
            FoliaDelayedTask abstractTask = new FoliaDelayedTask(runnable, delay, getExecutionContextType());
            ScheduledTask foliaTask = REGION_SCHEDULER.runDelayed(plugin, world, chunkX, chunkZ, traceFolia(runnable, abstractTask), TickTemporalUnit.toTicks(delay));
            abstractTask.setFoliaTask(foliaTask);
            return abstractTask;
        }

        @NotNull
        @Override
        public DelayedRepeatingTask repeating(@NotNull Duration initialDelay, @NotNull Duration intervalDelays, @NotNull Consumer<DelayedRepeatingTask> runnable) {
            FoliaDelayedRepeatingTask abstractTask = new FoliaDelayedRepeatingTask(runnable, initialDelay, intervalDelays, getExecutionContextType());
            ScheduledTask foliaTask = REGION_SCHEDULER.runAtFixedRate(plugin, world, chunkX, chunkZ, traceFolia(runnable, abstractTask), normalizeTicks(initialDelay), TickTemporalUnit.toTicks(intervalDelays));
            abstractTask.setFoliaTask(foliaTask);
            return abstractTask;
        }
    }
}
