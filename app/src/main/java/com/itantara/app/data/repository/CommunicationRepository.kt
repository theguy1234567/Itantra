package com.itantara.app.data.repository

import com.itantara.app.data.model.TextMessage
import com.itantara.app.network.ConnectionState
import com.itantara.app.network.ITantraWebSocketClient
import com.itantara.app.network.ITantraWebSocketServer
import com.itantara.app.network.LocalNetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URI
import java.util.UUID

object CommunicationRepository {

    val deviceId: String = UUID.randomUUID().toString().take(8)

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableStateFlow<List<TextMessage>>(emptyList())
    val messages: StateFlow<List<TextMessage>> = _messages.asStateFlow()

    private var server: ITantraWebSocketServer? = null
    private var client: ITantraWebSocketClient? = null

    fun getLocalIpAddress(): String? {
        return LocalNetworkUtils.getLocalIpAddress()
    }

    fun startHost(port: Int = 8765) {
        stopAll()
        val localIp = getLocalIpAddress()
        if (localIp.isNullOrBlank()) {
            _connectionState.value = ConnectionState.Error("No local network address found. Connect to Wi-Fi/Hotspot first.")
            return
        }

        try {
            server = ITantraWebSocketServer(
                port = port,
                onClientConnected = { clientAddress ->
                    _connectionState.value = ConnectionState.Connected(clientAddress, isHost = true)
                },
                onClientDisconnected = { reason ->
                    _connectionState.value = ConnectionState.Disconnected(reason)
                },
                onMessageReceived = { jsonStr ->
                    handleIncomingMessage(jsonStr)
                },
                onErrorOccurred = { errorMsg ->
                    _connectionState.value = ConnectionState.Error(errorMsg)
                }
            )
            server?.start()
            _connectionState.value = ConnectionState.HostWaiting(localIp, port)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Failed to start server: ${e.message}")
        }
    }

    fun connectToHost(hostIp: String, port: Int = 8765) {
        stopAll()
        if (hostIp.isBlank()) {
            _connectionState.value = ConnectionState.Error("Please enter a valid Host IP address")
            return
        }

        _connectionState.value = ConnectionState.Connecting(hostIp, port)
        repositoryScope.launch {
            try {
                val uri = URI("ws://$hostIp:$port")
                client = ITantraWebSocketClient(
                    serverUri = uri,
                    onConnected = {
                        _connectionState.value = ConnectionState.Connected(hostIp, isHost = false)
                    },
                    onDisconnected = { reason ->
                        _connectionState.value = ConnectionState.Disconnected(reason)
                    },
                    onMessageReceived = { jsonStr ->
                        handleIncomingMessage(jsonStr)
                    },
                    onErrorOccurred = { errorMsg ->
                        _connectionState.value = ConnectionState.Error(errorMsg)
                    }
                )
                client?.connect()
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error("Invalid IP or connection failure: ${e.message}")
            }
        }
    }

    fun sendMessage(text: String): Boolean {
        if (text.isBlank()) return false
        val message = TextMessage(senderId = deviceId, text = text)
        val jsonStr = message.toJson()

        var sentSuccess = false
        val currentState = _connectionState.value

        if (currentState is ConnectionState.Connected) {
            if (currentState.isHost) {
                sentSuccess = server?.sendToClient(jsonStr) ?: false
            } else {
                sentSuccess = client?.sendMessageSafely(jsonStr) ?: false
            }
        }

        if (sentSuccess) {
            _messages.value = _messages.value + message
        }

        return sentSuccess
    }

    private fun handleIncomingMessage(jsonStr: String) {
        val message = TextMessage.fromJson(jsonStr)
        if (message != null) {
            _messages.value = _messages.value + message
        }
    }

    fun stopAll() {
        try {
            server?.stopServerGracefully()
            server = null
            client?.disconnectGracefully()
            client = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _connectionState.value = ConnectionState.Idle
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}
