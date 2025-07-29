package org.kingdoms.abstraction

interface Cancellable {
    /**
     * @return true if the cancellation was successful, otherwise false. Should return true if already cancelled.
     */
    fun cancel(): Boolean
    fun isCancelled(): Boolean
}