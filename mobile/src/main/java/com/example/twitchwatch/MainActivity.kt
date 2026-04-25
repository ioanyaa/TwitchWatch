package com.example.twitchwatch

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var ircService: TwitchIrcService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            ircService = (binder as TwitchIrcService.LocalBinder).getService()
            ircService?.onNewMessage = { msg ->
                Log.d("CHAT", "${msg.author}: ${msg.text}")
            }
            bound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) { bound = false }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        Intent(this, TwitchIrcService::class.java).also { intent ->
            ContextCompat.startForegroundService(this, intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) unbindService(connection)
    }
}
