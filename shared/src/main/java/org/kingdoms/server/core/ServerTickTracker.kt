package org.kingdoms.server.core

interface ServerTickTracker {
    // Using int, it can track up to:
    // (((2147483647 / 20) / 60) / 60) / 24
    // 1243 days
    val ticks: Int
}