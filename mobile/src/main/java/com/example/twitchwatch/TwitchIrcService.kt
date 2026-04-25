package com.example.twitchwatch

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.*

class TwitchIrcService : Service() {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var started = false

    private val notificationId = 1001
    private val notificationChannelId = "twitch_irc"

    // TODO: replace with OAuth later
    private val token = "oauth:TOKENUL_TAU"
    private val nick = "USERUL_TAU"
    private val channel = "#CHANNELUL_TAU"

    inner class LocalBinder : Binder() {
        fun getService() = this@TwitchIrcService
    }
    private val binder = LocalBinder()

    var onNewMessage: (ChatMessage) -> Unit = {}

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForeground()
        if (!started) {
            started = true
            connect()
        }
        return START_STICKY
    }

    fun connect() {
        webSocket?.cancel()

        val request = Request.Builder()
            .url("wss://irc-ws.chat.twitch.tv:443")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                ws.send("CAP REQ :twitch.tv/tags twitch.tv/commands")
                ws.send("PASS $token")
                ws.send("NICK $nick")
                ws.send("JOIN $channel")
                Log.d("IRC", "Connected to $channel")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                text.lines().forEach { line ->
                    when {
                        line.contains("PRIVMSG") -> handleChat(line)
                        line.startsWith("PING") -> {
                            ws.send("PONG :tmi.twitch.tv")
                            Log.d("IRC", "PONG sent")
                        }
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("IRC", "Error: ${t.message}")
                Handler(Looper.getMainLooper()).postDelayed({ connect() }, 5000)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d("IRC", "Closed: $reason")
            }
        })
    }

    private fun ensureForeground() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = nm.getNotificationChannel(notificationChannelId)
            if (existing == null) {
                val channel = NotificationChannel(
                    notificationChannelId,
                    "Twitch IRC",
                    NotificationManager.IMPORTANCE_LOW
                )
                nm.createNotificationChannel(channel)
            }
        }

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Twitch chat connected")
            .setContentText("Listening to $channel")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(notificationId, notification)
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
        webSocket?.close(1000, "Service stopped")
        client.dispatcher.executorService.shutdown()
        started = false
    }
}

data class ChatMessage(
    val author: String,
    val text: String,
    val color: String,
    val timestamp: Long
)
