package org.kingdoms.utils.cache

import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class RunnableCountDownLatch(countdown: Int, private val runnable: Consumer<RunnableCountDownLatch>) {
    private val countdown: AtomicInteger
    val total: AtomicInteger

    init {
        require(countdown > 0) { "Countdown number must be greater than zero" }
        this.countdown = AtomicInteger(countdown)
        this.total = AtomicInteger(countdown)
    }

    fun increase() {
        countdown.incrementAndGet()
        total.incrementAndGet()
    }

    fun countDown() {
        if (countdown.get() <= 0) throw IllegalStateException("Already down to zero: " + countdown.get())
        if (countdown.decrementAndGet() == 0) runnable.accept(this)
    }
}