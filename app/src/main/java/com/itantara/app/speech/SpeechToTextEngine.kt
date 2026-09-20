package com.itantara.app.speech

interface SpeechToTextEngine {
    /**
     * Starts listening for audio input.
     */
    fun startRecording()

    /**
     * Stops listening and returns the transcribed text.
     */
    suspend fun stopRecordingAndTranscribe(): String?

    /**
     * Releases any resources associated with the STT engine.
     */
    fun release()
}
