package com.itantara.app.network

sealed class ConnectionState {
    object Idle : ConnectionState()
    data class HostWaiting(val ip: String, val port: Int) : ConnectionState()
    data class Connecting(val targetIp: String, val port: Int) : ConnectionState()
    data class Connected(val remoteAddress: String, val isHost: Boolean) : ConnectionState()
    data class Disconnected(val reason: String = "") : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
