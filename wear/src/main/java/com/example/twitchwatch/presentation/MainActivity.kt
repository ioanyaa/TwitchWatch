package com.example.twitchwatch

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private var ircClient: TwitchIrcClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val channelInput = findViewById<EditText>(R.id.channelInput)
        val connectButton = findViewById<Button>(R.id.connectButton)
        val statusText = findViewById<TextView>(R.id.statusText)

        connectButton.setOnClickListener {
            val channel = channelInput.text.toString().trim().lowercase()
            if (channel.isEmpty()) return@setOnClickListener

            ircClient?.disconnect()
            statusText.text = "Conectare la #$channel..."

            ircClient = TwitchIrcClient(channel) { msg ->
                runOnUiThread {
                    statusText.text = "Live: #$channel — ${msg.author}: ${msg.text}"
                }
                WearDataBridge.sendMessage(applicationContext, msg.author, msg.text, msg.color)
            }
            ircClient?.connect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ircClient?.disconnect()
    }
}