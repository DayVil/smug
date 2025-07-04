package com.github.smugapp.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import kotlinx.serialization.Serializable
import java.util.UUID

private const val TAG = "HomeScreen"

private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
private val CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

@Serializable
object HomeScreenRoute

@SuppressLint("MissingPermission")
@Composable
fun ConnectionScreenContent(
    bluetoothLEDiscoveryHandler: BluetoothLEDiscoveryHandler,
    bluetoothLEConnectionHandler: BluetoothLEConnectionHandler
) {
    var weightState by remember { mutableIntStateOf(0) }
    val bleDevices by bluetoothLEDiscoveryHandler
        .discoveredDevices
        .collectAsState()
    var isConnecting by remember { mutableStateOf(false) }

    val connectionState by bluetoothLEConnectionHandler.connectionState.collectAsState()
    val discoveredServices by bluetoothLEConnectionHandler.discoveredServices.collectAsState()

    // Automatically start scanning when component is first composed
    LaunchedEffect(Unit) {
        Log.d(TAG, "Starting automatic BLE scan for device with service $SERVICE_UUID")
        bluetoothLEDiscoveryHandler.startScan()
    }

    // Automatically connect to device when discovered
    LaunchedEffect(bleDevices, connectionState, isConnecting) {
        if (connectionState == ConnectionState.DISCONNECTED && !isConnecting && bleDevices.isNotEmpty()) {
            // Find the first BLE device that might be our target device
            val targetDevice = bleDevices.firstOrNull { device ->
                device.name != null && device.type == BluetoothDevice.DEVICE_TYPE_LE
            }

            if (targetDevice != null) {
                Log.d(
                    TAG,
                    "Found potential target device: ${targetDevice.name} (${targetDevice.address})"
                )
                Log.d(TAG, "Attempting to connect to device...")
                isConnecting = true
                bluetoothLEConnectionHandler.connect(targetDevice)
            }
        }
    }

    // Handle connection state changes and service discovery
    LaunchedEffect(connectionState, discoveredServices) {
        when (connectionState) {
            ConnectionState.CONNECTED -> {
                Log.d(TAG, "Connected to device")
                isConnecting = false

                if (discoveredServices.isEmpty()) {
                    Log.d(TAG, "No services discovered yet, ensuring discovery")
                    bluetoothLEConnectionHandler.ensureServicesDiscovered()
                    return@LaunchedEffect
                }

                Log.d(TAG, "Discovered Services are available. Checking for target service.")
                Log.d(TAG, "Discovered Services: ${discoveredServices.map { it.uuid }}")

                val serviceExists = discoveredServices.any { it.uuid == SERVICE_UUID }
                if (!serviceExists) {
                    Log.w(
                        TAG,
                        "Target service $SERVICE_UUID not found. This device may not be the weight scale. Continuing scan..."
                    )
                    // Continue scanning for the correct device
                    bluetoothLEDiscoveryHandler.startScan()
                    return@LaunchedEffect
                }

                Log.d(TAG, "Target service found! Setting up characteristic notifications.")
                val success = bluetoothLEConnectionHandler.setCharacteristicNotification(
                    serviceUuid = SERVICE_UUID,
                    characteristicUuid = CHARACTERISTIC_UUID,
                    enable = true
                ) { data ->
                    Log.d(
                        TAG,
                        "Received weight data: ${
                            data.joinToString(", ") {
                                String.format(
                                    "%02X",
                                    it
                                )
                            }
                        } " +
                                "which translates to: ${String(data)}"
                    )
                    try {
                        val newWeight = String(data).toInt()
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            weightState = newWeight
                        }
                    } catch (e: NumberFormatException) {
                        Log.e(TAG, "Failed to parse weight data: ${String(data)}", e)
                    }
                }

                if (success) {
                    Log.d(TAG, "Successfully subscribed to weight notifications")
                    // Stop scanning since we found and connected to our device
                    bluetoothLEDiscoveryHandler.stopScan()
                } else {
                    Log.e(TAG, "Failed to subscribe to weight notifications")
                }
            }

            ConnectionState.DISCONNECTED -> {
                Log.d(TAG, "Disconnected from device")
                isConnecting = false
                // Restart scanning when disconnected
                bluetoothLEDiscoveryHandler.startScan()
            }

            ConnectionState.CONNECTING -> {
                Log.d(TAG, "Connecting to device...")
            }

            ConnectionState.DISCONNECTING -> {
                Log.d(TAG, "Disconnecting from device...")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp)
        ) {
            Text(text = "Weight: $weightState")
            Text(text = "Connection State: $connectionState")

            Spacer(modifier = Modifier.height(16.dp))

            // Show scanning status
            when (connectionState) {
                ConnectionState.DISCONNECTED -> {
                    Text(text = "Scanning for weight scale...")
                    Text(text = "Devices found: ${bleDevices.size}")
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
    }
}