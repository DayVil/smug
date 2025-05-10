package com.github.smugapp.network

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission

class BluetoothHandler(private val context: Context) {
    private val bluetoothManager: BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter

    private val TAG = "BluetoothHandler"

    init {
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        this.bluetoothManager = bluetoothManager

        val bluetoothAdapter = bluetoothManager.adapter
        this.bluetoothAdapter = bluetoothAdapter
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun isBluetoothEnabled(): Boolean {
        if (!this.bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            this.context.startActivity(enableBtIntent)
        }

        return this.bluetoothAdapter.isEnabled
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getPairedDevices(): Set<BluetoothDevice> {
        this.bluetoothAdapter.bondedDevices.forEach {
            Log.d(this.TAG, "Device: ${it.name}, ${it.address}")
        }
        return this.bluetoothAdapter.bondedDevices
    }
}
