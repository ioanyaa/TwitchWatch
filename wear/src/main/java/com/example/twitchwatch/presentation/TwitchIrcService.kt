package com.example.twitchwatch

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import okhttp3.*

class TwitchIrcService : Service() {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    // TODO: înlocuiește cu OAuth mai târziu
    private val token = "oauth:TOKENUL_TAU"
    private val nick = "USERUL_TAU"
    private val channel = "#CHANNELUL_TAU"

    // Binder ca MainActivity să poată asculta mesaje
    inner class LocalBinder : Binder() {
        fun getService() = this@TwitchIrcService
    }
    private val binder = LocalBinder()

    var onNewMessage: (ChatMessage) -> Unit = {}

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connect()
        return START_STICKY
    }

    fun connect() {
        val request = Request.Builder()
            .url("wss://irc-ws.chat.twitch.tv:443")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                ws.send("CAP REQ :twitch.tv/tags twitch.tv/commands")
                ws.send("PASS $token")
                ws.send("NICK $nick")
                ws.send("JOIN $channel")
                Log.d("IRC", "Conectat la $channel")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                text.lines().forEach { line ->
                    when {
                        line.contains("PRIVMSG") -> handleChat(line)
                        line.startsWith("PING") -> {
                            ws.send("PONG :tmi.twitch.tv")
                            Log.d("IRC", "PONG trimis")
                        }
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("IRC", "Eroare: ${t.message}")
                Handler(Looper.getMainLooper()).postDelayed({ connect() }, 5000)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d("IRC", "Închis: $reason")
            }
        })
    }

    private fun handleChat(line: String) {
        val author = line.substringAfter(":").substringBefore("!")
        val message = line.substringAfter("PRIVMSG $channel :").trim()
        val color = extractTag(line, "color").ifEmpty { "#9147FF" }

        val chatMessage = ChatMessage(
            author = author,
            text = message,
            color = color,
            timestamp = System.currentTimeMillis()
        )

        Log.d("IRC", "${chatMessage.author}: ${chatMessage.text}")
        onNewMessage(chatMessage)
    }

    private fun extractTag(line: String, tag: String): String {
        return line.substringAfter("$tag=")
            .substringBefore(";")
            .substringBefore(" ")
    }

    override fun onDestroy() {
        webSocket?.close(1000, "Service oprit")
        client.dispatcher.executorService.shutdown()
    }
}

data class ChatMessage(
    val author: String,
    val text: String,
    val color: String,
    val timestamp: Long
)