package com.example.twitchwatch

import android.util.Log
import okhttp3.*

class TwitchIrcClient(
    private val channel: String,
    private val onMessage: (ChatMessage) -> Unit
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun connect() {
        val nick = "justinfan${(10000..99999).random()}"
        val request = Request.Builder()
            .url("wss://irc-ws.chat.twitch.tv:443")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                ws.send("NICK $nick")
                ws.send("JOIN #$channel")
                Log.d("IRC", "Conectat la #$channel")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                text.lines().forEach { line ->
                    when {
                        line.contains("PRIVMSG") -> {
                            val author = line.substringAfter(":").substringBefore("!")
                            val message = line.substringAfter("PRIVMSG #$channel :").trim()
                            if (author.isNotEmpty() && message.isNotEmpty()) {
                                Log.d("IRC", "$author: $message")
                                onMessage(ChatMessage(author, message, "#9147FF"))
                            }
                        }
                        line.startsWith("PING") -> ws.send("PONG :tmi.twitch.tv")
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("IRC", "Error: ${t.message}")
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d("IRC", "Closed: $reason")
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "disconnect")
        client.dispatcher.executorService.shutdown()
    }
}

data class ChatMessage(
    val author: String,
    val text: String,
    val color: String = "#9147FF",
    val timestamp: Long = System.currentTimeMillis()
)