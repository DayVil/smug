package com.github.smugapp.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.github.smugapp.data.SmugRepo
import com.github.smugapp.model.DrinkProduct
import com.github.smugapp.network.ConnectionState
import com.github.smugapp.network.ble.BluetoothLEConnectionHandler
import com.github.smugapp.network.ble.BluetoothLEDiscoveryHandler
import com.github.smugapp.network.off.OffService
import com.github.smugapp.network.off.SearchState
import com.github.smugapp.network.off.parseInput
import com.github.smugapp.ui.components.CameraPreview
import com.github.smugapp.ui.components.OffCard
import com.github.smugapp.ui.components.ScannerSearchBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

private const val TAG = "ScannerAndWeightScreen"
private const val PREFS_NAME = "smug_app_prefs"
private const val KEY_ZERO_OFFSET = "zero_offset"
private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
private val CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

@Serializable
object BarCodeScannerRoute

@SuppressLint("MissingPermission")
@Composable
fun BarCodeScannerContent(
    repo: SmugRepo,
    bluetoothLEDiscoveryHandler: BluetoothLEDiscoveryHandler,
    bluetoothLEConnectionHandler: BluetoothLEConnectionHandler
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var rawWeight by remember { mutableIntStateOf(0) }
    var zeroOffset by remember { mutableIntStateOf(sharedPrefs.getInt(KEY_ZERO_OFFSET, 0)) }
    val weightState = rawWeight - zeroOffset

    val bleDevices by bluetoothLEDiscoveryHandler.discoveredDevices.collectAsState()
    var isConnecting by remember { mutableStateOf(false) }
    val connectionState by bluetoothLEConnectionHandler.connectionState.collectAsState()
    val discoveredServices by bluetoothLEConnectionHandler.discoveredServices.collectAsState()

    var productId by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val client = remember { OffService() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by client.searchState.collectAsState()
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<DrinkProduct?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { bluetoothLEDiscoveryHandler.startScan() }

    LaunchedEffect(bleDevices, connectionState, isConnecting) {
        if (connectionState == ConnectionState.DISCONNECTED && !isConnecting && bleDevices.isNotEmpty()) {
            val targetDevice = bleDevices.firstOrNull { it.name != null && it.type == BluetoothDevice.DEVICE_TYPE_LE }
            if (targetDevice != null) {
                isConnecting = true
                bluetoothLEConnectionHandler.connect(targetDevice)
            }
        }
    }

    LaunchedEffect(connectionState, discoveredServices) {
        if (connectionState == ConnectionState.CONNECTED) {
            isConnecting = false
            if (discoveredServices.isEmpty()) {
                bluetoothLEConnectionHandler.ensureServicesDiscovered()
                return@LaunchedEffect
            }
            if (!discoveredServices.any { it.uuid == SERVICE_UUID }) {
                bluetoothLEDiscoveryHandler.startScan()
                return@LaunchedEffect
            }
            val success = bluetoothLEConnectionHandler.setCharacteristicNotification(
                serviceUuid = SERVICE_UUID,
                characteristicUuid = CHARACTERISTIC_UUID,
                enable = true
            ) { data ->
                try {
                    val weightInMilliGrams = String(data).toInt()
                    val weightInGrams = (weightInMilliGrams *(-1)) / 1000
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        rawWeight = weightInGrams
                    }
                } catch (e: NumberFormatException) {
                    Log.e(TAG, "Failed to parse weight data: ${String(data)}", e)
                }
            }
            if (success) bluetoothLEDiscoveryHandler.stopScan()
        } else if (connectionState == ConnectionState.DISCONNECTED) {
            isConnecting = false
            bluetoothLEDiscoveryHandler.startScan()
        }
    }

    LaunchedEffect(connectionState) {
        val message = when (connectionState) {
            ConnectionState.CONNECTING -> "Connecting to scale..."
            ConnectionState.CONNECTED -> "Scale connected"
            ConnectionState.DISCONNECTED -> "Scale disconnected. Searching..."
            ConnectionState.DISCONNECTING -> "Disconnecting from scale..."
        }
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    DisposableEffect(Unit) { onDispose { client.close() } }

    // --- Confirmation Dialog (Updated Logic) ---
    if (showConfirmationDialog && selectedProduct != null) {
        // Determine the amount to be logged. Default to 1 if weight is 0 or less.
        val amountToLog = if (weightState > 0) weightState else 1

        AlertDialog(
            onDismissRequest = {
                showConfirmationDialog = false
                selectedProduct = null
            },
            title = { Text(text = "Confirm Drink") },
            // Update the dialog text to reflect the actual amount that will be saved.
            text = { Text(text = "Add ${amountToLog}g of '${selectedProduct!!.getSensibleName()}' to your log?") },
            confirmButton = {
                Button(
                    onClick = {
                        val productToSave = selectedProduct
                        // Use the pre-calculated amountToLog, which handles the default case.
                        val consumedAmount = amountToLog
                        if (productToSave != null) {
                            val scaleFactor = consumedAmount / 100.0
                            val scaledNutrients = productToSave.nutrients?.copy(
                                caloriesPer100g = (productToSave.nutrients.caloriesPer100g ?: 0.0) * scaleFactor,
                                sugarsPer100g = (productToSave.nutrients.sugarsPer100g ?: 0.0) * scaleFactor,
                                caffeinePer100g = (productToSave.nutrients.caffeinePer100g ?: 0.0) * scaleFactor,
                                saturatedFatPer100g = (productToSave.nutrients.saturatedFatPer100g ?: 0.0) * scaleFactor
                            )
                            val finalProduct = productToSave.copy(
                                createdAt = System.currentTimeMillis(),
                                consumedAmount = consumedAmount,
                                nutrients = scaledNutrients
                            )
                            scope.launch { repo.insertDrinkProduct(finalProduct) }
                        }
                        showConfirmationDialog = false
                        selectedProduct = null
                    },
                    // Button is always enabled since a weight of 0 is now a valid case.
                    enabled = true
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmationDialog = false
                    selectedProduct = null
                }) { Text("Cancel") }
            }
        )
    }

    fun searchAction() {
        if (productId.isBlank()) return
        if (uiState is SearchState.Loading) return
        val searchTerm = parseInput(productId)
        scope.launch { client.searchProduct(searchTerm) }
        keyboardController?.hide()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        if (isScanning) {
            ScanRunning(
                onBarcodeDetected = { barcode ->
                    productId = barcode
                    isScanning = false
                    scope.launch {
                        delay(100)
                        searchAction()
                    }
                },
                setScanning = { isScanning = false }
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(start = 20.dp, top = 16.dp, end = 20.dp)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF63A128))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Weight: $weightState g", color = Color.White)
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(onClick = {
                                val newOffset = rawWeight
                                sharedPrefs.edit().putInt(KEY_ZERO_OFFSET, newOffset).apply()
                                zeroOffset = newOffset
                            }) { Text("Zero") }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { rawWeight = Random.nextInt(50, 500) }) {
                        Text("Simulate Weight")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(24.dp))
                    ScannerSearchBox(
                        productId = productId,
                        setProductId = { productId = it },
                        uiState = uiState,
                        onScanAction = { isScanning = true },
                        searchAction = { searchAction() }
                    )
                    when (val state = uiState) {
                        is SearchState.Loading -> Text("Loading...", Modifier.padding(top = 16.dp))
                        is SearchState.Success -> {
                            if (state.products.isEmpty() && state.isComplete) {
                                Text("No products found", Modifier.padding(top = 16.dp))
                            } else {
                                LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                                    items(state.products.size) { index ->
                                        val product = state.products[index]
                                        Box(modifier = Modifier.clickable {
                                            selectedProduct = product
                                            showConfirmationDialog = true
                                        }) { OffCard(product) }
                                    }
                                }
                            }
                        }
                        is SearchState.Error -> Text("Error: ${state.reason}", Modifier.padding(top = 16.dp))
                        SearchState.Init -> Text("Enter a product ID or scan a barcode", Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ScanRunning(onBarcodeDetected: (String) -> Unit, setScanning: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(onBarcodeDetected = onBarcodeDetected, modifier = Modifier.fillMaxSize())
        IconButton(
            onClick = setScanning,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }
        Text(
            "Point camera at barcode to scan",
            Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            color = Color.White
        )
    }
}