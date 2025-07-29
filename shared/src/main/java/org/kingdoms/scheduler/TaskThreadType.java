package org.kingdoms.scheduler;

public enum TaskThreadType {
    /**
     * Task must run synchronously. It must change to the server thread if not already in it
     * otherwise can run if its already in the thread.
     */
    SYNC,

    /**
     * Task must run asynchronously. It must change to an async thread if not already in it,
     * otherwise can run if it's already in an async thread.
     */
    ASYNC,

    /**
     * Run the task in any thread that's most optimized (mostly, but not necessarily the current thread)
     * regardless if it's sync or async.
     */
    ANY;
}
