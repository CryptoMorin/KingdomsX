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

package org.kingdoms.scheduler;

import org.kingdoms.server.core.Server;

import java.time.Duration;

/**
 * A scheduler for running tasks using the systems provided by the platform
 */
public interface TaskScheduleProvider {
    /**
     * Gets an async executor instance
     *
     * @return an async executor instance
     */
    TaskScheduler async();

    /**
     * Gets a sync executor instance
     *
     * @return a sync executor instance
     */
    TaskScheduler sync();

    void initCache();

    /**
     * A quick way to run the specified task in an appropriate thread.
     * If the current thread already meets the requirement, it's executed
     * in the current thread.
     */
    default void run(TaskThreadType threadType, Runnable runnable) {
        if (threadType == TaskThreadType.ANY) {
            runnable.run();
            return;
        }

        boolean isMainThread = Server.get().isMainThread();
        boolean async = threadType == TaskThreadType.ASYNC;
        if (isMainThread == !async) {
            runnable.run();
            return;
        }

        TaskScheduler scheduler = async ? async() : sync();
        scheduler.execute(runnable);
    }

    default TaskScheduler getTaskScheduler(TaskThreadType threadType) {
        boolean isMainThread = Server.get().isMainThread();

        if (threadType == TaskThreadType.ANY) {
            return new InstantOrDelayedTaskScheduler(isMainThread ? sync() : async());
        }

        boolean async = threadType == TaskThreadType.ASYNC;
        if (isMainThread == !async) {
            return new InstantOrDelayedTaskScheduler(isMainThread ? sync() : async());
        }

        return async ? async() : sync();
    }

    boolean isShutdown();

    /**
     * Shuts down the scheduler instance.
     *
     * <p>{@link TaskScheduler#delayed(Duration, Runnable)} and {@link TaskScheduler#repeating(Duration, Duration, Runnable)}.</p>
     */
    void shutdown();
}
