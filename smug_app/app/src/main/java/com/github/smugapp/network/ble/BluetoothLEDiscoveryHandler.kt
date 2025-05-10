package com.github.smugapp.network.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.github.smugapp.checkPermissions
import com.github.smugapp.fetchPermissions
import com.github.smugapp.isLocationEnabled
import com.github.smugapp.network.NetworkDiscovery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "BluetoothLEDiscoveryHandler"
private const val SCAN_PERIOD = 10000L // 10 seconds

class BluetoothLEDiscoveryHandler(private val context: Context) : NetworkDiscovery {
    private val _discoveredDevices = MutableStateFlow<Set<BluetoothDevice>>(emptySet())
    val discoveredDevices: StateFlow<Set<BluetoothDevice>> = _discoveredDevices

    private val bluetoothManager: BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())

    private val leScanCallback = BluetoothLEDiscoveryCallback(_discoveredDevices)

    init {
        Log.d(TAG, "Initializing BluetoothLEHandler")
        try {
            bluetoothManager = context.getSystemService(BluetoothManager::class.java)
            Log.d(TAG, "BluetoothManager initialized successfully")

            bluetoothAdapter = bluetoothManager.adapter
            Log.d(TAG, "BluetoothAdapter initialized successfully")

            bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            Log.d(
                TAG,
                "BluetoothLeScanner ${if (bluetoothLeScanner != null) "initialized successfully" else "failed to initialize"}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing BluetoothLEHandler: ${e.message}", e)
            throw e
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun isBluetoothEnabled(): Boolean {
        Log.d(TAG, "Checking if Bluetooth is enabled")
        return bluetoothAdapter.isEnabled
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun startScan(): Boolean {
        Log.d(TAG, "Starting BLE scan")

        if (!checkPermissions(fetchPermissions(), context)) {
            Log.e(TAG, "Cannot start scan: Missing required permissions")
            return false
        }

        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Cannot start scan: Bluetooth is not enabled")
            return false
        }

        if (!isLocationEnabled(context)) {
            Log.e(TAG, "Cannot start scan: Location services are not enabled")
            return false
        }

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "Cannot start scan: BluetoothLeScanner is null")
            return false
        }

        if (scanning) {
            Log.d(TAG, "Scan already in progress")
            return true
        }

        _discoveredDevices.value = emptySet()

        handler.postDelayed({
            if (scanning) {
                scanning = false
                stopScan()
                Log.d(TAG, "BLE scan stopped after timeout")
            }
        }, SCAN_PERIOD)

        try {
            // Create scan settings for low latency (more battery usage but faster discovery)
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            // Can add filters here if needed
            val filters = ArrayList<ScanFilter>()

            Log.d(TAG, "Starting BLE scan with settings: ${settings.scanMode}")
            bluetoothLeScanner?.startScan(filters, settings, leScanCallback)
            scanning = true

            Log.d(TAG, "BLE scan started successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE scan: ${e.message}", e)
            return false
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun stopScan() {
        if (!scanning) {
            Log.d(TAG, "No BLE scan in progress to stop")
            return
        }

        try {
            bluetoothLeScanner?.stopScan(leScanCallback)
            scanning = false
            Log.d(TAG, "BLE scan stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BLE scan: ${e.message}", e)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun cleanup() {
        Log.d(TAG, "Cleaning up BluetoothLEHandler")
        if (scanning) {
            stopScan()
        }
        handler.removeCallbacksAndMessages(null)
    }
} 