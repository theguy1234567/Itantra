package com.itantara.app.network

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

class ITantraWebSocketServer(
    port: Int = 8765,
    private val onClientConnected: (String) -> Unit,
    private val onClientDisconnected: (String) -> Unit,
    private val onMessageReceived: (String) -> Unit,
    private val onErrorOccurred: (String) -> Unit
) : WebSocketServer(InetSocketAddress("0.0.0.0", port)) {

    private var activeConnection: WebSocket? = null

    init {
        isReuseAddr = true
    }

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        conn?.let {
            activeConnection = it
            val remoteIp = it.remoteSocketAddress?.address?.hostAddress ?: "Client"
            onClientConnected(remoteIp)
        }
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        if (conn == activeConnection) {
            activeConnection = null
            onClientDisconnected(reason ?: "Client disconnected")
        }
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        message?.let {
            onMessageReceived(it)
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        val errorMsg = ex?.message ?: "Server socket error"
        onErrorOccurred(errorMsg)
    }

    override fun onStart() {
        // Server started successfully
    }

    fun sendToClient(message: String): Boolean {
        return try {
            activeConnection?.let {
                if (it.isOpen) {
                    it.send(message)
                    true
                } else false
            } ?: false
        } catch (e: Exception) {
            onErrorOccurred("Failed to send message: ${e.message}")
            false
        }
    }

    fun stopServerGracefully() {
        try {
            activeConnection?.close()
            activeConnection = null
            stop(1000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
