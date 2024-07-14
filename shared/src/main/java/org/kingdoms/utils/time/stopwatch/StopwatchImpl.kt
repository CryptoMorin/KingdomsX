package org.kingdoms.utils.time.stopwatch

import org.kingdoms.server.core.Server
import java.time.Duration

class TickStopwatch(ticks: Int = 0) : AbstractStopwatch(ticks.toLong()) {
    fun elapsedTicks(): Int = super.passed.toInt()
    override fun getCurrentTime(): Long = Server.get().ticks.toLong()
}

class MillisStopwatch(passed: Duration = Duration.ZERO) : AbstractStopwatch(passed.toMillis()) {
    override fun getCurrentTime(): Long = System.currentTimeMillis()
}