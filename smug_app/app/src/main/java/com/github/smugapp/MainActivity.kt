package com.github.smugapp

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.smugapp.network.BluetoothDiscoveryHandler
import com.github.smugapp.ui.theme.SmugAppTheme
import kotlinx.serialization.Serializable

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


@SuppressLint("MissingPermission")
@Composable
fun HomeScreenContent(
    weight: Int,
    bluetoothHandler: BluetoothDiscoveryHandler
) {
    val discoveredDevices by bluetoothHandler.discoveredDevices.collectAsState()
    Log.d("HomeScreenContent", "Rendering with ${discoveredDevices.size} discovered devices")

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Weight: $weight")
                
                Button(
                    onClick = {
                        bluetoothHandler.startDiscovery()
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text("Start Discovery")
                }

                LazyColumn {
                    items(discoveredDevices.toList()) { device ->
                        Text(
                            text = "${device.name ?: "Unknown Device"} (${device.address})",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Serializable
object HomeScreen