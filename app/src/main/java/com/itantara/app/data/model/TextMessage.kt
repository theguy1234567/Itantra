package com.itantara.app.data.model

import org.json.JSONObject
import java.util.UUID

data class TextMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: String = "TEXT",
    val text: String
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("messageId", messageId)
        json.put("senderId", senderId)
        json.put("timestamp", timestamp)
        json.put("messageType", messageType)
        json.put("text", text)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): TextMessage? {
            return try {
                val json = JSONObject(jsonStr)
                TextMessage(
                    messageId = json.optString("messageId", UUID.randomUUID().toString()),
                    senderId = json.optString("senderId", "unknown"),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                    messageType = json.optString("messageType", "TEXT"),
                    text = json.optString("text", "")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
