package com.itantara.app.network

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class ITantraWebSocketClient(
    serverUri: URI,
    private val onConnected: () -> Unit,
    private val onDisconnected: (String) -> Unit,
    private val onMessageReceived: (String) -> Unit,
    private val onErrorOccurred: (String) -> Unit
) : WebSocketClient(serverUri) {

    override fun onOpen(handshakedata: ServerHandshake?) {
        onConnected()
    }

    override fun onMessage(message: String?) {
        message?.let {
            onMessageReceived(it)
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        onDisconnected(reason?.ifBlank { "Disconnected from server" } ?: "Connection closed")
    }

    override fun onError(ex: Exception?) {
        onErrorOccurred(ex?.message ?: "Client connection error")
    }

    fun sendMessageSafely(message: String): Boolean {
        return try {
            if (isOpen) {
                send(message)
                true
            } else {
                onErrorOccurred("Socket is not open")
                false
            }
        } catch (e: Exception) {
            onErrorOccurred("Failed to send message: ${e.message}")
            false
        }
    }

    fun disconnectGracefully() {
        try {
            close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
