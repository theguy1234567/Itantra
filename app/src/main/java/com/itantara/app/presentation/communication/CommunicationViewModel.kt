package com.itantara.app.presentation.communication

import androidx.lifecycle.ViewModel
import com.itantara.app.data.model.TextMessage
import com.itantara.app.data.repository.CommunicationRepository
import com.itantara.app.network.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CommunicationViewModel : ViewModel() {

    private val repository = CommunicationRepository

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val messages: StateFlow<List<TextMessage>> = repository.messages

    val currentDeviceId: String = repository.deviceId

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isNotBlank()) {
            if (repository.sendMessage(text)) {
                _inputText.value = ""
            }
        }
    }
}
