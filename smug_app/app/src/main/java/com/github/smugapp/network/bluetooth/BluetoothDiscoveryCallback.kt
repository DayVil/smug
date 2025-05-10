package com.github.smugapp.network.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow

private const val TAG = "BluetoothDiscoveryHandler"

class BluetoothDiscoveryCallback(
    private val discoveredDevices: MutableStateFlow<Set<BluetoothDevice>>,
    private val stopDiscovery: () -> Unit
) : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_FOUND -> {
                Log.d(TAG, "Device found broadcast received")
                val device =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                device?.let {
                    val currentDevices = discoveredDevices.value.toMutableSet()
                    currentDevices.add(it)
                    discoveredDevices.value = currentDevices
                    Log.d(TAG, "Found device: ${it.name ?: "Unknown"} (${it.address})")
                } ?: Log.e(TAG, "Received ACTION_FOUND but device was null")
            }

            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                Log.d(TAG, "Discovery finished broadcast received")
                stopDiscovery()
            }

            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                Log.d(TAG, "Discovery started broadcast received")
            }

            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                val stateString = when (state) {
                    BluetoothAdapter.STATE_OFF -> "STATE_OFF"
                    BluetoothAdapter.STATE_TURNING_OFF -> "STATE_TURNING_OFF"
                    BluetoothAdapter.STATE_ON -> "STATE_ON"
                    BluetoothAdapter.STATE_TURNING_ON -> "STATE_TURNING_ON"
                    else -> "UNKNOWN_STATE"
                }
                Log.d(TAG, "Bluetooth state changed to: $stateString")
            }
        }
    }
}