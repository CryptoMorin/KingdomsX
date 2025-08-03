package org.kingdoms.scheduler.convert;

import java.util.function.Consumer;

public class UnconsumedRunnable<T> implements Consumer<T> {
    private final Runnable runnable;

    public UnconsumedRunnable(Runnable runnable) {this.runnable = runnable;}

    public Runnable getRunnable() {
        return runnable;
    }

    @Override
    public void accept(T t) {
        runnable.run();
    }
}
