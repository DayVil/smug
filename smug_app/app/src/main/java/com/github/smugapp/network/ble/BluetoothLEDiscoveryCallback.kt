package com.github.smugapp.network.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow

private const val TAG = "BluetoothLEDiscoveryCallback"

class BluetoothLEDiscoveryCallback(private val _discoveredDevices: MutableStateFlow<Set<BluetoothDevice>>) :
    ScanCallback() {
    override fun onScanResult(callbackType: Int, result: ScanResult) {
        super.onScanResult(callbackType, result)
        val device = result.device
        Log.d(TAG, "BLE Device found: ${device.address} - RSSI: ${result.rssi}")

        val currentDevices = _discoveredDevices.value.toMutableSet()
        currentDevices.add(device)
        _discoveredDevices.value = currentDevices
    }

    override fun onBatchScanResults(results: MutableList<ScanResult>) {
        super.onBatchScanResults(results)
        Log.d(TAG, "Batch scan results received: ${results.size} devices")

        val currentDevices = _discoveredDevices.value.toMutableSet()
        for (result in results) {
            currentDevices.add(result.device)
            Log.d(TAG, "BLE Device found: ${result.device.address} - RSSI: ${result.rssi}")
        }
        _discoveredDevices.value = currentDevices
    }

    override fun onScanFailed(errorCode: Int) {
        super.onScanFailed(errorCode)
        val errorMessage = when (errorCode) {
            SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
            SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Application registration failed"
            SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE not supported"
            SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
            else -> "Unknown error code: $errorCode"
        }
        Log.e(TAG, "BLE Scan failed: $errorMessage")
    }
}