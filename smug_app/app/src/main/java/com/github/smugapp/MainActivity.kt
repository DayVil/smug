package com.github.smugapp

import android.Manifest
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
import com.github.smugapp.network.BluetoothDiscoveryHandler
import com.github.smugapp.ui.screens.HomeScreen
import com.github.smugapp.ui.screens.HomeScreenContent
import com.github.smugapp.ui.theme.SmugAppTheme

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothHandler: BluetoothDiscoveryHandler

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions(this)
        
        try {
            bluetoothHandler = BluetoothDiscoveryHandler(this)
            Log.d(TAG, "BluetoothHandler initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize BluetoothHandler", e)
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
                        HomeScreenContent(weight, bluetoothHandler)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bluetoothHandler.isInitialized) {
            bluetoothHandler.cleanup()
        }
    }
}




