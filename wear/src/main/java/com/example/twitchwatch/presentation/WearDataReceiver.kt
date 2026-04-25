package com.example.twitchwatch.presentation

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class WearDataReceiver : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/chat") {
                val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                val author = map.getString("author") ?: return@forEach
                val text = map.getString("text") ?: return@forEach
                val color = map.getString("color") ?: "#9147FF"
                ChatRepository.addMessage(ChatMessage(author, text, color))
            }
        }
    }
}