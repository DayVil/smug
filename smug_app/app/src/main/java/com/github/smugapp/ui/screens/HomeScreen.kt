package com.github.smugapp.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.smugapp.network.BluetoothDiscoveryHandler
import kotlinx.serialization.Serializable

@Serializable
object HomeScreen

@SuppressLint("MissingPermission")
@Composable
fun HomeScreenContent(
    weight: Int,
    bluetoothHandler: BluetoothDiscoveryHandler
) {
    val discoveredDevices by bluetoothHandler.discoveredDevices.collectAsState()
    Log.d("HomeScreenContent", "Rendering with ${discoveredDevices.size} discovered devices")

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Weight: $weight")

                Button(
                    onClick = {
                        bluetoothHandler.startDiscovery()
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text("Start Discovery")
                }

                LazyColumn {
                    items(discoveredDevices.toList()) { device ->
                        Text(
                            text = "${device.name ?: "Unknown Device"} (${device.address})",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}