@file:Suppress("NOTHING_TO_INLINE")

package org.kingdoms.utils.time

import java.time.Duration

object TimeExtensions {
    @JvmStatic fun Duration.assertPositive(): Duration {
        if (this.isNegative) throw IllegalStateException("Duration must be positive: $this")
        return this
    }
}