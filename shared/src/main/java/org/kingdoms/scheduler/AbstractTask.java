package org.kingdoms.scheduler;

import org.jetbrains.annotations.NotNull;
import org.kingdoms.utils.internal.functional.Fn;

import java.util.function.Consumer;

public class AbstractTask implements Task {
    private final Task.ExecutionContextType executionContextType;
    private final Consumer<Task> runnable;

    public AbstractTask(@NotNull Task.ExecutionContextType executionContextType, @NotNull Consumer<Task> runnable) {
        this.executionContextType = executionContextType;
        this.runnable = Fn.cast(runnable);
    }

    @Override
    public Task.ExecutionContextType getExecutionContextType() {
        return this.executionContextType;
    }

    @Override
    public void run() {
        this.runnable.accept(this);
    }

    @Override
    public void accept(Task task) {
        runnable.accept(task);
    }
}