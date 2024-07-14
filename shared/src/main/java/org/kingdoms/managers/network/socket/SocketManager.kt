package org.kingdoms.managers.network.socket

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.constants.namespace.Namespaced
import org.kingdoms.constants.namespace.NamespacedRegistry
import org.kingdoms.utils.network.SocketJsonCommunicator
import java.util.function.Consumer
import java.util.logging.Logger

class SocketManager(val logger: Logger) : NamespacedRegistry<SocketHandler>() {
    companion object {
        @JvmStatic private var INSTANCE: SocketManager? = null
        @JvmStatic fun initMainManager(logger: Logger) {
            INSTANCE = SocketManager(logger)
        }

        @JvmStatic fun getMain(): SocketManager = INSTANCE!!
    }

    fun sendRequest(request: Consumer<JsonObject>) {
        val jsonObject = JsonObject()
        request.accept(jsonObject)
        socket.send(jsonObject)
    }

    override fun register(value: SocketHandler) {
        super.register(value)
        socket.log("Registered handler: " + value.namespace)
    }

    val socket = object : SocketJsonCommunicator(4343, logger) {
        override fun onReceive(data: JsonElement) {
            require(data is JsonObject) { "Cannot deserialize non-object socket" }
            val nsElement = data["namespace"] ?: throw NullPointerException("Namespace element not available")
            val ns = Namespace.fromString(nsElement.asString)
            val handler = registry[ns] ?: throw IllegalArgumentException("Unknown socket handler: $ns")
            val dataElement = data["data"] ?: throw NullPointerException("Missing data element for: $ns")
            val requestId: String? = data["id"]?.asString
            if (handler.needsRequestId && requestId == null) throw NullPointerException("Missing request ID for: $ns")

            handler.onReceive(handler.SocketSession(this@SocketManager, dataElement, requestId))
        }
    }
}

abstract class SocketHandler(private val ns: Namespace, val needsRequestId: Boolean = false) :
    Namespaced {
    abstract fun onReceive(session: SocketSession)
    override fun getNamespace() = ns

    inner class SocketSession(val socket: SocketManager, val data: JsonElement, val requestId: String?) {
        fun reply(sendData: JsonElement) {
            socket.sendRequest { container ->
                if (needsRequestId) container.addProperty("id", requestId)
                container.addProperty("namespace", ns.asNormalizedString())
                container.add("data", sendData)
            }
        }
    }
}