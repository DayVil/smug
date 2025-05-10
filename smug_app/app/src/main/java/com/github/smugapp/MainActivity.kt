package com.github.smugapp

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.smugapp.ui.theme.SmugAppTheme
import kotlinx.serialization.Serializable
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.smugapp.network.BluetoothHandler


class MainActivity : ComponentActivity() {
    private lateinit var bluetoothHandler: BluetoothHandler
    private val TAG = "MainActivity"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        Log.d(TAG, "Permission request result: ${if (allGranted) "all granted" else "some denied"}")
        if (!allGranted) {
            Log.w(TAG, "Some permissions were denied. Bluetooth functionality may be limited.")
            // Log which permissions were denied
            permissions.entries.forEach { (permission, granted) ->
                if (!granted) {
                    Log.w(TAG, "Permission denied: $permission")
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        Log.d(TAG, "Checking and requesting permissions")
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(TAG, "Using Android 12+ permissions")
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            Log.d(TAG, "Using legacy permissions")
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: ${permissionsToRequest.joinToString()}")
            requestPermissionLauncher.launch(permissionsToRequest)
        } else {
            Log.d(TAG, "All required permissions are already granted")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        
        checkAndRequestPermissions()
        
        try {
            bluetoothHandler = BluetoothHandler(this)
            Log.d(TAG, "BluetoothHandler initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize BluetoothHandler", e)
        }

        enableEdgeToEdge()
        setContent {
            SmugAppTheme {
                val navController = rememberNavController()
                var weight by remember { mutableIntStateOf(0) }

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
        Log.d(TAG, "onDestroy called")
        super.onDestroy()
        if (::bluetoothHandler.isInitialized) {
            bluetoothHandler.cleanup()
        }
    }
}


@Composable
fun HomeScreenContent(
    weight: Int,
    bluetoothHandler: BluetoothHandler
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
                        Log.d("HomeScreenContent", "Start Discovery button clicked")
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