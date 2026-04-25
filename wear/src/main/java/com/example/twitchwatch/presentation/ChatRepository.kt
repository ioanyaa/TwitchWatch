package com.example.twitchwatch.presentation

import androidx.compose.runtime.mutableStateListOf

data class ChatMessage(
    val author: String,
    val text: String,
    val color: String = "#9147FF"
)

object ChatRepository {
    val messages = mutableStateListOf<ChatMessage>()

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        if (messages.size > 50) messages.removeAt(0)
    }
}