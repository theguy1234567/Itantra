package com.itantara.app.presentation.connection

import androidx.lifecycle.ViewModel
import com.itantara.app.data.repository.CommunicationRepository
import com.itantara.app.network.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionMode { HOST, JOIN }

class ConnectionViewModel : ViewModel() {

    private val repository = CommunicationRepository

    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    private val _selectedMode = MutableStateFlow(ConnectionMode.HOST)
    val selectedMode: StateFlow<ConnectionMode> = _selectedMode.asStateFlow()

    private val _targetIp = MutableStateFlow("")
    val targetIp: StateFlow<String> = _targetIp.asStateFlow()

    private val _targetPort = MutableStateFlow("8765")
    val targetPort: StateFlow<String> = _targetPort.asStateFlow()

    fun selectMode(mode: ConnectionMode) {
        _selectedMode.value = mode
    }

    fun updateTargetIp(ip: String) {
        _targetIp.value = ip
    }

    fun updateTargetPort(port: String) {
        _targetPort.value = port
    }

    fun getLocalIp(): String? {
        return repository.getLocalIpAddress()
    }

    fun startHost() {
        val port = _targetPort.value.toIntOrNull() ?: 8765
        repository.startHost(port)
    }

    fun connectToHost() {
        val port = _targetPort.value.toIntOrNull() ?: 8765
        repository.connectToHost(_targetIp.value.trim(), port)
    }

    fun disconnect() {
        repository.stopAll()
    }
}
