package org.kingdoms.constants.namespace

class NamespacedFlags(private val defaults: NamespacedFlags?) : NamespacedMap<Boolean>() {
    override fun get(namespace: Namespace): Boolean {
        return super.get(namespace) ?: defaults?.get(namespace) ?: false
    }
}