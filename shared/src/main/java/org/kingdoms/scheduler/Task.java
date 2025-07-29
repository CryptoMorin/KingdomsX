package org.kingdoms.scheduler;

import java.util.function.Consumer;

public interface Task extends Runnable, Consumer<Task> {
    enum ExecutionContextType {SYNC, ASYNC}

    ExecutionContextType getExecutionContextType();
}