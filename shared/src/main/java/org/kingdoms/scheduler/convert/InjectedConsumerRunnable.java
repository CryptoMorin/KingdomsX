package org.kingdoms.scheduler.convert;

import java.util.function.Consumer;

public class InjectedConsumerRunnable<T> implements Runnable {
    private final Consumer<T> consumer;
    private final T parameter;

    public InjectedConsumerRunnable(Consumer<T> consumer, T parameter) {
        this.consumer = consumer;
        this.parameter = parameter;
    }

    @Override
    public void run() {
        consumer.accept(parameter);
    }
}
