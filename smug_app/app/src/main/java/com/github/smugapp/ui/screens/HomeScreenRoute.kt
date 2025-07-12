package com.github.smugapp.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
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
import androidx.compose.material3.Button
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
import com.github.smugapp.ui.components.ConnectionNotif
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.random.Random

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
    // State for the actual sensor value
    var rawWeight by remember { mutableIntStateOf(0) }
    // State for the tare offset
    var zeroOffset by remember { mutableIntStateOf(0) }
    // The final weight to display, calculated after applying the zero offset
    val weightState = rawWeight - zeroOffset

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
            val targetDevice = bleDevices.firstOrNull { device ->
                device.name != null && device.type == BluetoothDevice.DEVICE_TYPE_LE
            }
            if (targetDevice != null) {
                Log.d(TAG, "Found potential target device: ${targetDevice.name} (${targetDevice.address})")
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
                    bluetoothLEConnectionHandler.ensureServicesDiscovered()
                    return@LaunchedEffect
                }
                val serviceExists = discoveredServices.any { it.uuid == SERVICE_UUID }
                if (!serviceExists) {
                    Log.w(TAG, "Target service $SERVICE_UUID not found. Continuing scan...")
                    bluetoothLEDiscoveryHandler.startScan()
                    return@LaunchedEffect
                }
                Log.d(TAG, "Target service found! Setting up characteristic notifications.")
                val success = bluetoothLEConnectionHandler.setCharacteristicNotification(
                    serviceUuid = SERVICE_UUID,
                    characteristicUuid = CHARACTERISTIC_UUID,
                    enable = true
                ) { data ->
                    Log.d(TAG, "Received weight data: ${String(data)}")
                    try {
                        val newWeight = String(data).toInt()
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            // Update the raw weight from the sensor
                            rawWeight = newWeight
                        }
                    } catch (e: NumberFormatException) {
                        Log.e(TAG, "Failed to parse weight data: ${String(data)}", e)
                    }
                }
                if (success) {
                    Log.d(TAG, "Successfully subscribed to weight notifications")
                    bluetoothLEDiscoveryHandler.stopScan()
                } else {
                    Log.e(TAG, "Failed to subscribe to weight notifications")
                }
            }
            ConnectionState.DISCONNECTED -> {
                Log.d(TAG, "Disconnected from device")
                isConnecting = false
                bluetoothLEDiscoveryHandler.startScan()
            }
            else -> { /* Handle other states if necessary */ }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp)
        ) {
            ConnectionNotif(connectionState)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Weight: $weightState")
            Spacer(modifier = Modifier.height(16.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = {
                    // Set the current raw weight as the offset
                    zeroOffset = rawWeight
                }) {
                    Text("Zero")
                }
                Spacer(modifier = Modifier.width(16.dp))
                // This button simulates a new weight reading from the ESP32
                Button(onClick = {
                    // Update raw weight with a new random value to simulate a change
                    rawWeight = Random.nextInt(100, 1000)
                }) {
                    Text("Simulate Weight")
                }
            }
        }
    }
}