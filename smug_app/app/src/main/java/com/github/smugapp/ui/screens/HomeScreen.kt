package com.github.smugapp.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.smugapp.network.ConnectionState
import com.github.smugapp.network.ble.BluetoothLEConnectionHandler
import com.github.smugapp.network.ble.BluetoothLEDiscoveryHandler
import com.github.smugapp.ui.components.DeviceCard
import kotlinx.serialization.Serializable
import java.util.UUID

private const val TAG = "HomeScreen"

private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
private val CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

@Serializable
object HomeScreen

@SuppressLint("MissingPermission")
@Composable
fun HomeScreenContent(
    weight: Int,
    bluetoothLEDiscoveryHandler: BluetoothLEDiscoveryHandler,
    bluetoothLEConnectionHandler: BluetoothLEConnectionHandler
) {
    var weightState by remember { mutableIntStateOf(weight) }
    val bleDevices by bluetoothLEDiscoveryHandler
        .discoveredDevices
        .collectAsState()
    var selectedDevice: BluetoothDevice? by remember { mutableStateOf(null) }

    val connectionState by bluetoothLEConnectionHandler.connectionState.collectAsState()
    val discoveredServices by bluetoothLEConnectionHandler.discoveredServices.collectAsState()

    LaunchedEffect(connectionState, discoveredServices) {
        if (connectionState == ConnectionState.CONNECTED) {
            Log.d(TAG, "Connected state detected")

            if (discoveredServices.isEmpty()) {
                Log.d(TAG, "No services discovered yet, ensuring discovery")
                bluetoothLEConnectionHandler.ensureServicesDiscovered()
                return@LaunchedEffect
            }

            Log.d(TAG, "Discovered Services are available. Attempting to set notification.")
            Log.d(TAG, "Discovered Services: ${discoveredServices.map { it.uuid }}")

            val serviceExists = discoveredServices.any { it.uuid == SERVICE_UUID }
            if (!serviceExists) {
                Log.e(TAG, "Target service $SERVICE_UUID not found in discovered services")
                return@LaunchedEffect
            }

            val success = bluetoothLEConnectionHandler.setCharacteristicNotification(
                serviceUuid = SERVICE_UUID,
                characteristicUuid = CHARACTERISTIC_UUID,
                enable = true
            ) {
                Log.d(
                    TAG,
                    "Received data: ${it.joinToString(", ") { String.format("%02X", it) }} " +
                            "which translates into: ${String(it)}"
                )
                val newWeight = String(it).toInt()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    weightState = newWeight
                }
            }
            if (success) {
                Log.d(
                    "HomeScreen",
                    "Successfully subscribed to notifications for $CHARACTERISTIC_UUID"
                )
            } else {
                Log.e(
                    "HomeScreen",
                    "Failed to subscribe to notifications for $CHARACTERISTIC_UUID"
                )
            }
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Weight: $weightState")
                Text(text = "Connection State: $connectionState")

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Button(
                        onClick = { bluetoothLEDiscoveryHandler.startScan() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text("Start BLE Scan") }

                    Button(
                        onClick = { bluetoothLEDiscoveryHandler.stopScan() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text("Stop BLE Scan") }
                }

                LazyColumn {
                    items(
                        bleDevices
                            .toList()
                            .filter {
                                it.name != null && it.type == BluetoothDevice.DEVICE_TYPE_LE
                            }) { device ->
                        DeviceCard(
                            device = device,
                            selectedDevice = selectedDevice,
                        ) {
                            selectedDevice = device
                            bluetoothLEConnectionHandler.connect(device)
                        }
                    }
                }
            }
        }
    }
}