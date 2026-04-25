package com.example.twitchwatch

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private var ircService: TwitchIrcService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            ircService = (binder as TwitchIrcService.LocalBinder).getService()
            bound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) { bound = false }
    }

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

        Intent(this, TwitchIrcService::class.java).also { intent ->
            startService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        connectButton.setOnClickListener {
            val channel = channelInput.text.toString().trim().lowercase()
            if (channel.isEmpty()) return@setOnClickListener

            ircService?.setChannel(channel)
            ircService?.onNewMessage = { msg ->
                Log.d("CHAT", "${msg.author}: ${msg.text}")
                runOnUiThread {
                    statusText.text = "Live: #$channel"
                }
                WearDataBridge.sendMessage(applicationContext, msg.author, msg.text, msg.color)
            }
            statusText.text = "Se conectează la #$channel..."
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) unbindService(connection)
    }
}