package com.itantara.app.presentation.communication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.itantara.app.data.model.TextMessage
import com.itantara.app.network.ConnectionState

@Composable
fun CommunicationScreen(viewModel: CommunicationViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()

    val isConnected = connectionState is ConnectionState.Connected
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "iTANTRA",
                    style = MaterialTheme.typography.titleLarge
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .width(10.dp)
                            .height(10.dp)
                            .background(
                                color = if (isConnected) Color.Green else Color.Red,
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = if (isConnected) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Message list area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages yet. Type below to start talking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { msg ->
                            val isMe = msg.senderId == viewModel.currentDeviceId
                            MessageBubble(message = msg, isMe = isMe)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text input area
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.updateInputText(it) },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = isConnected
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.sendMessage() },
                    enabled = isConnected && inputText.isNotBlank()
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: TextMessage, isMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (isMe) "Me" else "Remote",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
