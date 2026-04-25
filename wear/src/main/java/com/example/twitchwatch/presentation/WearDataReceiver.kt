package com.example.twitchwatch.presentation

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class WearDataReceiver : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            val item = DataMapItem.fromDataItem(event.dataItem).dataMap
            val author = item.getString("author") ?: return@forEach
            val text = item.getString("text") ?: return@forEach
            val color = item.getString("color") ?: "#9147FF"
            ChatRepository.addMessage(ChatMessage(author, text, color))
        }
    }
}