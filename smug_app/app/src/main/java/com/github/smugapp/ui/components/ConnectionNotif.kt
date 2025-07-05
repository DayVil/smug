package com.github.smugapp.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.smugapp.network.ConnectionState

@Composable
fun ConnectionNotif(connectionState: ConnectionState) {
    Text(text = "Connection State: $connectionState")

    Spacer(modifier = Modifier.height(16.dp))

    // Show scanning status
    when (connectionState) {
        ConnectionState.DISCONNECTED -> {
            Text(text = "Scanning for weight scale...")
        }

        ConnectionState.CONNECTING -> {
            Text(text = "Connecting to weight scale...")
        }

        ConnectionState.CONNECTED -> {
            Text(text = "Connected to weight scale")
        }

        ConnectionState.DISCONNECTING -> {
            Text(text = "Disconnecting...")
        }
    }
}