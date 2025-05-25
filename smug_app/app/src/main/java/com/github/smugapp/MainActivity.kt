package com.github.smugapp

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresPermission
import com.github.smugapp.data.DrinkDb
import com.github.smugapp.data.DrinkRepo
import com.github.smugapp.network.ble.BluetoothLEConnectionHandler
import com.github.smugapp.network.ble.BluetoothLEDiscoveryHandler
import com.github.smugapp.ui.screens.MainContent

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothLEDiscoveryHandler: BluetoothLEDiscoveryHandler
    private lateinit var bluetoothLEConnectionHandler: BluetoothLEConnectionHandler
    private lateinit var mainContent: MainContent

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions(this)
        
        try {
            bluetoothLEDiscoveryHandler = BluetoothLEDiscoveryHandler(this)
            bluetoothLEConnectionHandler = BluetoothLEConnectionHandler(this)

            Log.d(TAG, "Bluetooth handlers initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Bluetooth handlers", e)
        }

        val dao = DrinkDb.getDatabase(this).drinkDao()
        val repo = DrinkRepo(dao)

        mainContent = MainContent(
            this,
            bluetoothLEDiscoveryHandler,
            bluetoothLEConnectionHandler,
            repo
        )
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        if (::bluetoothLEDiscoveryHandler.isInitialized) {
            bluetoothLEDiscoveryHandler.cleanup()
        }
        if (::bluetoothLEConnectionHandler.isInitialized) {
            bluetoothLEConnectionHandler.cleanup()
        }
    }
}




