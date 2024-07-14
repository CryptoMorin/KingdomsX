package org.kingdoms.tasks

enum class TaskState {
    CONTINUE, ERROR, SHOULD_STOP, MUST_STOP;

    fun shouldStop(): Boolean = this != CONTINUE
}