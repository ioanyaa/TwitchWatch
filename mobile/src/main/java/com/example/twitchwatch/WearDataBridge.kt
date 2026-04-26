package com.example.twitchwatch

import android.content.Context
import android.util.Log
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

object WearDataBridge {
    private val db = Firebase.database.reference

    fun sendMessage(context: Context, author: String, text: String, color: String) {
        val message = mapOf(
            "author" to author,
            "text" to text,
            "color" to color,
            "timestamp" to ServerValue.TIMESTAMP
        )
        db.child("twitch/latest").setValue(message)
        db.child("twitch/history").push().setValue(message)
        Log.d("WearDataBridge", "Trimis Firebase: $author: $text")
    }
}