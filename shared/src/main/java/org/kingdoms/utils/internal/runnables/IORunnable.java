package org.kingdoms.utils.internal.runnables;

import java.io.IOException;

@FunctionalInterface
public interface IORunnable {
    void run() throws IOException;
}
