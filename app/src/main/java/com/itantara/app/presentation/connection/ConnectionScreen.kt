package com.itantara.app.presentation.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.itantara.app.network.ConnectionState

@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel,
    onNavigateToCommunication: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val targetIp by viewModel.targetIp.collectAsState()
    val targetPort by viewModel.targetPort.collectAsState()

    val isConnected = connectionState is ConnectionState.Connected
    val localIp = viewModel.getLocalIp() ?: "No Wi-Fi/Hotspot IP"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "iTANTRA",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Local Device Connection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Mode Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = selectedMode == ConnectionMode.HOST,
                    onClick = { viewModel.selectMode(ConnectionMode.HOST) },
                    label = { Text("Host (Create Room)") }
                )
                Spacer(modifier = Modifier.width(12.dp))
                FilterChip(
                    selected = selectedMode == ConnectionMode.JOIN,
                    onClick = { viewModel.selectMode(ConnectionMode.JOIN) },
                    label = { Text("Join (Connect)") }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedMode == ConnectionMode.HOST) {
                        Text(
                            text = "Host Server Configuration",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Your Local IP: $localIp",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Port: $targetPort",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (connectionState is ConnectionState.Idle || connectionState is ConnectionState.Error || connectionState is ConnectionState.Disconnected) {
                            Button(
                                onClick = { viewModel.startHost() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Start Server")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.disconnect() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Stop Server")
                            }
                        }
                    } else {
                        Text(
                            text = "Join Host Device",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = targetIp,
                            onValueChange = { viewModel.updateTargetIp(it) },
                            label = { Text("Host IP Address (e.g. 192.168.43.1)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = targetPort,
                            onValueChange = { viewModel.updateTargetPort(it) },
                            label = { Text("Port") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isConnected && connectionState !is ConnectionState.Connecting) {
                            Button(
                                onClick = { viewModel.connectToHost() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Connect")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.disconnect() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Disconnect")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status display
            val statusText = when (val state = connectionState) {
                is ConnectionState.Idle -> "Status: Idle"
                is ConnectionState.HostWaiting -> "Status: Server running. Waiting for device at ${state.ip}:${state.port}..."
                is ConnectionState.Connecting -> "Status: Connecting to ${state.targetIp}:${state.port}..."
                is ConnectionState.Connected -> "Status: Connected to ${state.remoteAddress}"
                is ConnectionState.Disconnected -> "Status: Disconnected (${state.reason})"
                is ConnectionState.Error -> "Status Error: ${state.message}"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Bottom Continue button (Enabled ONLY when connected per Constraint 4)
        Button(
            onClick = onNavigateToCommunication,
            enabled = isConnected,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Enter Communication")
        }
    }
}
