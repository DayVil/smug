package com.github.smugapp.network

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothHandler(private val context: Context) {
    private val bluetoothManager: BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter
    private val TAG = "BluetoothHandler"

    // StateFlow to emit discovered devices
    private val _discoveredDevices = MutableStateFlow<Set<BluetoothDevice>>(emptySet())
    val discoveredDevices: StateFlow<Set<BluetoothDevice>> = _discoveredDevices

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    Log.d(TAG, "Device found broadcast received")
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    
                    device?.let {
                        val currentDevices = _discoveredDevices.value.toMutableSet()
                        currentDevices.add(it)
                        _discoveredDevices.value = currentDevices
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
                    val stateString = when(state) {
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

    init {
        Log.d(TAG, "Initializing BluetoothHandler")
        try {
            val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
            this.bluetoothManager = bluetoothManager
            Log.d(TAG, "BluetoothManager initialized successfully")

            val bluetoothAdapter = bluetoothManager.adapter
            this.bluetoothAdapter = bluetoothAdapter
            Log.d(TAG, "BluetoothAdapter initialized successfully")

            // Register for broadcasts when a device is discovered
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            context.registerReceiver(receiver, filter)
            Log.d(TAG, "BroadcastReceiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing BluetoothHandler: ${e.message}", e)
            throw e
        }
    }

    private fun checkPermissions(): Boolean {
        Log.d(TAG, "Checking required permissions")
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            Log.e(TAG, "Missing permissions: ${missingPermissions.joinToString()}")
            return false
        }
        
        Log.d(TAG, "All required permissions are granted")
        return true
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun isBluetoothEnabled(): Boolean {
        Log.d(TAG, "Checking if Bluetooth is enabled")
        if (!this.bluetoothAdapter.isEnabled) {
            Log.d(TAG, "Bluetooth is disabled, requesting enable")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            this.context.startActivity(enableBtIntent)
            return false
        }
        Log.d(TAG, "Bluetooth is enabled")
        return true
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        
        Log.d(TAG, "Location services - GPS: $isGpsEnabled, Network: $isNetworkEnabled")
        
        if (!isGpsEnabled && !isNetworkEnabled) {
            Log.d(TAG, "Location services are disabled")
            // Open location settings
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            context.startActivity(intent)
            return false
        }
        return true
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    fun startDiscovery(): Boolean {
        Log.d(TAG, "Starting Bluetooth discovery")
        
        // Check permissions first
        if (!checkPermissions()) {
            Log.e(TAG, "Cannot start discovery: Missing required permissions")
            return false
        }
        
        // Check if Bluetooth is enabled
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Cannot start discovery: Bluetooth is not enabled")
            return false
        }

        // Check if location services are enabled
        if (!isLocationEnabled()) {
            Log.e(TAG, "Cannot start discovery: Location services are not enabled")
            return false
        }

        // Cancel any ongoing discovery
        if (bluetoothAdapter.isDiscovering) {
            Log.d(TAG, "Discovery already in progress, canceling current discovery")
            bluetoothAdapter.cancelDiscovery()
        }

        // Clear previous discoveries
        _discoveredDevices.value = emptySet()
        
        // Start new discovery
        val success = bluetoothAdapter.startDiscovery()
        Log.d(TAG, "Discovery start result: ${if (success) "success" else "failed"}")
        
        if (!success) {
            Log.e(TAG, "Failed to start discovery. Possible reasons:")
            Log.e(TAG, "1. Bluetooth is not enabled")
            Log.e(TAG, "2. Location services are disabled")
            Log.e(TAG, "3. Missing required permissions")
            Log.e(TAG, "4. Another app is currently scanning")
            
            // Log current state for debugging
            Log.d(TAG, "Current state:")
            Log.d(TAG, "- Bluetooth enabled: ${bluetoothAdapter.isEnabled}")
            Log.d(TAG, "- Location enabled: ${isLocationEnabled()}")
            Log.d(TAG, "- Permissions granted: ${checkPermissions()}")
            Log.d(TAG, "- Discovery in progress: ${bluetoothAdapter.isDiscovering}")
        }
        
        return success
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    fun stopDiscovery() {
        Log.d(TAG, "Stopping Bluetooth discovery")
        if (bluetoothAdapter.isDiscovering) {
            val success = bluetoothAdapter.cancelDiscovery()
            Log.d(TAG, "Discovery stop result: ${if (success) "success" else "failed"}")
        } else {
            Log.d(TAG, "No discovery in progress to stop")
        }
    }

    fun cleanup() {
        Log.d(TAG, "Cleaning up BluetoothHandler")
        try {
            context.unregisterReceiver(receiver)
            Log.d(TAG, "BroadcastReceiver unregistered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}", e)
        }
    }
}
