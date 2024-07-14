package org.kingdoms.server.events

interface EventPropagator {
    @Suppress("INAPPLICABLE_JVM_NAME") @get:JvmName("shouldRegister") val shouldRegister: Boolean
}