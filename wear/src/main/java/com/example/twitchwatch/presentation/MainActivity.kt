package com.example.twitchwatch.presentation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.twitchwatch.presentation.theme.TwitchWatchTheme
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import android.content.Intent
class MainActivity : ComponentActivity(), SensorEventListener {

    private val db = Firebase.database.reference
    private val USER_ID = "demo_user_1"
    private val startTime = System.currentTimeMillis()

    private lateinit var sensorManager: SensorManager
    private var vibrator: Vibrator? = null

    private var isRaisingHand = false
    private var gestureStartTime = 0L
    private var isGoingDown = false
    private var squatStartTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this, SensorService::class.java))
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        setContent {
            TwitchWatchTheme {
                AppScaffold {
                    ChatScreen()
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(
                    "android.permission.health.READ_HEART_RATE",
                    android.Manifest.permission.BODY_SENSORS
                ),
                100
            )
        }

        setupFirebaseListeners()
        startSensors()
    }

    private fun setupFirebaseListeners() {
        db.child("twitch/latest").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val author = snapshot.child("author").getValue(String::class.java) ?: return
                val text = snapshot.child("text").getValue(String::class.java) ?: return
                val color = snapshot.child("color").getValue(String::class.java) ?: "#9147FF"
                Log.d("WearMain", "Mesaj primit: $author: $text")
                ChatRepository.addMessage(ChatMessage(author, text, color))
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        db.child("users/$USER_ID/notification").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val type = snapshot.child("type").getValue(String::class.java) ?: return
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                if (System.currentTimeMillis() - timestamp > 5000) return
                if (type == "SUB" || type == "FOLLOW") {
                    vibrateWatch()
                    Log.d("WearMain", "Sub/Follow detectat → vibratie")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun startSensors() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val hr = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        Log.d("WearMain", "Accelerometru: $accel")
        Log.d("WearMain", "HR sensor: $hr")

        accel?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        hr?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = Math.sqrt((x*x + y*y + z*z).toDouble()).toFloat()
                detectDrinkingGesture(y, magnitude)
                detectSquat(y, magnitude)
            }
            Sensor.TYPE_HEART_RATE -> {
                val hr = event.values[0].toInt()
                Log.d("WearMain", "HR raw: ${event.values[0]}")
                if (hr > 0) {
                    db.child("users/$USER_ID/vitals").updateChildren(
                        mapOf(
                            "hr" to hr,
                            "timestamp" to ServerValue.TIMESTAMP
                        )
                    )
                    Log.d("WearMain", "HR: $hr")
                }
            }
        }
    }

    private fun detectDrinkingGesture(y: Float, magnitude: Float) {
        val currentTime = System.currentTimeMillis()
        if (y > 3f && !isRaisingHand && magnitude > 5f) {
            isRaisingHand = true
            gestureStartTime = currentTime
        }
        if (isRaisingHand && y < -2f) {
            val duration = currentTime - gestureStartTime
            if (duration in 500..3000) {
                Log.d("WearMain", "Gest baut apa! Durata: ${duration}ms")
                confirmHydration()
            }
            isRaisingHand = false
        }
        if (isRaisingHand && currentTime - gestureStartTime > 4000) {
            isRaisingHand = false
        }
    }

    private fun confirmHydration() {
        val ref = db.child("users/$USER_ID/hydration/count")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val current = snapshot.getValue(Int::class.java) ?: 0
                if (current > 0) {
                    ref.setValue(current - 1)
                    Log.d("WearMain", "Hidratare confirmată! Count: ${current - 1}")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun detectSquat(y: Float, magnitude: Float) {
        val currentTime = System.currentTimeMillis()
        if (y < -3f && !isGoingDown && magnitude > 3f) {
            isGoingDown = true
            squatStartTime = currentTime
        }
        if (isGoingDown && y > 2f) {
            val duration = currentTime - squatStartTime
            if (duration in 300..3000) {
                Log.d("WearMain", "Squat detectat! Durata: ${duration}ms")
                confirmSquat()
            }
            isGoingDown = false
        }
        if (isGoingDown && currentTime - squatStartTime > 4000) {
            isGoingDown = false
        }
    }

    private fun confirmSquat() {
        val doneRef = db.child("users/$USER_ID/squats/done")
        doneRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val done = snapshot.getValue(Int::class.java) ?: 0
                doneRef.setValue(done + 1)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        val countRef = db.child("users/$USER_ID/squats/count")
        countRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.getValue(Int::class.java) ?: 0
                if (count > 0) {
                    countRef.setValue(count - 1)
                    Log.d("WearMain", "Squat confirmat! Count rămas: ${count - 1}")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun vibrateWatch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 200, 100, 200, 100, 400),
                    -1
                )
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}