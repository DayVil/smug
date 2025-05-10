package com.github.smugapp

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    private val bluetoothHandler = BluetoothHandler(this)

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                        HomeScreenContent(weight)
                    }
                }
            }
        }
    }
}


@Composable
fun HomeScreenContent(
    weight: Int,
) {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column {
                Text(text = "$weight")
            }
        }
    }
}

@Serializable
object HomeScreen