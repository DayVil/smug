package com.github.smugapp

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.smugapp.network.ble.BluetoothLEConnectionHandler
import com.github.smugapp.network.ble.BluetoothLEDiscoveryHandler
import com.github.smugapp.ui.screens.HomeScreen
import com.github.smugapp.ui.screens.HomeScreenContent
import com.github.smugapp.ui.theme.SmugAppTheme

//private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothLEDiscoveryHandler: BluetoothLEDiscoveryHandler
    private lateinit var bluetoothLEConnectionHandler: BluetoothLEConnectionHandler

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions(this)
        
        try {
            // Initialize both classic Bluetooth and BLE handlers
            bluetoothLEDiscoveryHandler = BluetoothLEDiscoveryHandler(this)
            bluetoothLEConnectionHandler = BluetoothLEConnectionHandler(this)

            Log.d(TAG, "Bluetooth handlers initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Bluetooth handlers", e)
        }

        enableEdgeToEdge()
        setContent {
            SmugAppTheme {
                val navController = rememberNavController()
                val weight by remember { mutableIntStateOf(0) }

                NavHost(
                    navController = navController,
                    startDestination = HomeScreen
                ) {
                    composable<HomeScreen> {
                        HomeScreenContent(
                            weight,
                            bluetoothLEDiscoveryHandler,
                            bluetoothLEConnectionHandler
                        )
                    }
                }
            }
        }
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




