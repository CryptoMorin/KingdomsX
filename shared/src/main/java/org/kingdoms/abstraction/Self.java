package org.kingdoms.abstraction;

public abstract class Self<SELF extends Self<SELF>> {
    protected final Self<SELF> self() {
        return this;
    }
}
