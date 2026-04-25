package com.example.twitchwatch.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Text
import com.example.twitchwatch.presentation.theme.TwitchWatchTheme

data class ChatMessage(
    val author: String,
    val text: String,
    val color: String = "#9147FF"
)

class MainActivity : ComponentActivity() {

    // mesaje fake pentru test vizual
    private val fakeMessages = listOf(
        ChatMessage("streamer_ro", "buna ziua chat!"),
        ChatMessage("fan_no1", "OMEGALUL", "#00b4d8"),
        ChatMessage("random_123", "ce gameplay bro", "#f77f00"),
        ChatMessage("chillguy99", "PogChamp", "#52b788"),
        ChatMessage("hyped_viewer", "lets gooo 🔥", "#e63946"),
        ChatMessage("mod_alex", "!clip", "#9147ff"),
        ChatMessage("lurker_guy", "HeyGuys buna seara", "#00b4d8"),
        ChatMessage("viewer_42", "cum se numeste jocul?", "#f77f00"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TwitchWatchTheme {
                AppScaffold {
                    ChatScreen(messages = fakeMessages)
                }
            }
        }
    }
}

@Composable
fun ChatScreen(messages: List<ChatMessage>) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                text = "● LIVE",
                color = Color(0xFF9147FF),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
            )
        }
        items(messages) { msg ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = msg.author,
                    color = Color(android.graphics.Color.parseColor(msg.color)),
                    fontSize = 9.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = ": ${msg.text}",
                    color = Color(0xFFCCCCCC),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}