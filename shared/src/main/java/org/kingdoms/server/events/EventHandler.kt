package org.kingdoms.server.events

interface EventHandler {
    fun callEvent(event: Any)
    fun registerEvents(container: Any)
    fun onLoad() {}
}

interface ServerEvent

interface AsyncEvent