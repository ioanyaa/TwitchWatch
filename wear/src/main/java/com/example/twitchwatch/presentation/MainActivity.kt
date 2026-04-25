package com.example.twitchwatch.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Text
import com.example.twitchwatch.presentation.theme.TwitchWatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TwitchWatchTheme {
                AppScaffold {
                    ChatScreen()
                }
            }
        }
    }
}

@Composable
fun ChatScreen() {
    val messages = ChatRepository.messages
    val listState = rememberScalingLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "● LIVE",
                    color = Color(0xFF9147FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        itemsIndexed(messages) { _, msg ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = msg.author,
                    color = Color(android.graphics.Color.parseColor(msg.color)),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
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