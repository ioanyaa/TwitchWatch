package com.example.twitchwatch

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import okhttp3.*

class TwitchIrcClient(
    private val channel: String,
    private val onMessage: (ChatMessage) -> Unit
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val db = Firebase.database.reference
    private val USER_ID = "demo_user_1"

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
                                processCommand(message.lowercase())
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

    private fun processCommand(message: String) {
        when {
            message.contains("bea apa") || message.contains("!water") -> {
                incrementCount("hydration/count")
                Log.d("IRC", "Bea apa detectat! +1")
            }
            message.contains("do squats") || message.contains("!squat") -> {
                incrementCount("squats/count")
                Log.d("IRC", "Squat detectat! +1")
            }
            message.contains("just subscribed") ||
                    message.contains("just gifted") ||
                    message.contains("subscribed with") -> {
                db.child("users/$USER_ID/notification").setValue(
                    mapOf(
                        "type" to "SUB",
                        "timestamp" to ServerValue.TIMESTAMP
                    )
                )
                Log.d("IRC", "Sub detectat → vibratie ceas")
            }
        }
    }

    private fun incrementCount(path: String) {
        val ref = db.child("users/$USER_ID/$path")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val current = snapshot.getValue(Int::class.java) ?: 0
                ref.setValue(current + 1)
            }
            override fun onCancelled(error: DatabaseError) {}
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