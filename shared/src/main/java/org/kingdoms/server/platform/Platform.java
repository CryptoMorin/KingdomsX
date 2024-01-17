package org.kingdoms.server.platform;

public enum Platform {
    BUKKIT(CrossPlatformNotifier.isRunningBukkit()),
    SPIGOT(CrossPlatformNotifier.isRunningSpigot()),
    FOLIA(CrossPlatformNotifier.isRunningFolia()),
    PAPER(CrossPlatformNotifier.isRunningPaper()),
    FORGE(CrossPlatformNotifier.isRunningForge()),
    BEDROCK(CrossPlatformNotifier.isRunningGeyser());

    private final boolean available;

    Platform(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }

    public static final Platform[] VALUES = values();
}
