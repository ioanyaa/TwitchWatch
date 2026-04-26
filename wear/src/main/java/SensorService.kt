package com.example.twitchwatch.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private val db = Firebase.database.reference
    private val USER_ID = "demo_user_1"

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(2, buildNotification())
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("SensorService", "HR sensor înregistrat")
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_HEART_RATE) return
        val hr = event.values[0].toInt()
        Log.d("SensorService", "HR raw: ${event.values[0]}")
        if (hr > 0) {
            db.child("users/$USER_ID/vitals").updateChildren(
                mapOf(
                    "hr" to hr,
                    "timestamp" to ServerValue.TIMESTAMP
                )
            )
            Log.d("SensorService", "HR uplodat: $hr")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "sensor_service",
            "Sensor Service",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, "sensor_service")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("TwitchWatch")
            .setContentText("Monitorizare activă...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
}