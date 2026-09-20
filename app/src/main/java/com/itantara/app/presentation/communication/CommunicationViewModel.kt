package com.itantara.app.presentation.communication

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantara.app.data.model.TextMessage
import com.itantara.app.data.repository.CommunicationRepository
import com.itantara.app.network.ConnectionState
import com.itantara.app.speech.SpeechToTextEngine
import com.itantara.app.speech.VakyanshOnnxSttEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommunicationViewModel : ViewModel() {

    private val repository = CommunicationRepository

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val messages: StateFlow<List<TextMessage>> = repository.messages

    val currentDeviceId: String = repository.deviceId

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private var sttEngine: SpeechToTextEngine? = null
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun initializeSpeechEngine(context: Context) {
        if (sttEngine == null) {
            sttEngine = VakyanshOnnxSttEngine(context)
        }
    }

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

    fun startRecording() {
        _isRecording.value = true
        sttEngine?.startRecording()
    }

    fun stopRecordingAndTranscribe() {
        _isRecording.value = false
        _isProcessing.value = true
        viewModelScope.launch {
            val text = sttEngine?.stopRecordingAndTranscribe()
            _isProcessing.value = false
            if (!text.isNullOrBlank()) {
                repository.sendMessage(text)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sttEngine?.release()
    }
}
