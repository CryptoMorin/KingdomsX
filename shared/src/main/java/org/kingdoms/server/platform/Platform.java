package org.kingdoms.server.platform;

public enum Platform {
    BUKKIT(CrossPlatformManager.isRunningBukkit()),
    SPIGOT(CrossPlatformManager.isRunningSpigot()),
    FOLIA(CrossPlatformManager.isRunningFolia()),
    PAPER(CrossPlatformManager.isRunningPaper()),
    FORGE(CrossPlatformManager.isRunningForge()),
    BEDROCK(CrossPlatformManager.isRunningGeyser());

    private final boolean available;

    Platform(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }

    public static final Platform[] VALUES = values();
}
