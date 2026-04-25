package com.example.twitchwatch

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

object WearDataBridge {
    fun sendMessage(context: Context, author: String, text: String, color: String) {
        val request = PutDataMapRequest.create("/chat").apply {
            dataMap.putString("author", author)
            dataMap.putString("text", text)
            dataMap.putString("color", color)
            dataMap.putLong("ts", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
    }
}