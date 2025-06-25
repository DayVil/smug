package com.github.smugapp.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.github.smugapp.data.SmugRepo
import com.github.smugapp.model.DrinkProduct
import com.github.smugapp.network.off.OffService
import com.github.smugapp.network.off.SearchState
import com.github.smugapp.network.off.parseInput
import com.github.smugapp.ui.components.CameraPreview
import com.github.smugapp.ui.components.OffCard
import com.github.smugapp.ui.components.ScannerSearchBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height


private const val TAG = "BarCodeScanner"

@Serializable
object BarCodeScannerRoute

@Composable
fun BarCodeScannerContent(repo: SmugRepo) {
    var productId by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val client = remember { OffService() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by client.searchState.collectAsState()

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<DrinkProduct?>(null) }

    // --- Start of new code: State for weight input ---
    var weightInput by remember { mutableStateOf("100") }
    // --- End of new code ---


    DisposableEffect(Unit) {
        onDispose {
            client.close()
            Log.d(TAG, "Client closed")
        }
    }

    // --- Start of modified code: Updated Confirmation Dialog ---
    if (showConfirmationDialog && selectedProduct != null) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text(text = "Add Drink") },
            text = {
                Column {
                    Text(text = "Enter the amount of '${selectedProduct!!.getSensibleName()}' consumed (in g/ml).")
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Amount (g/ml)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val productToSave = selectedProduct
                        val consumedAmount = weightInput.toIntOrNull() ?: 100

                        if (productToSave != null) {
                            // Scale the nutrients based on the input amount
                            val scaleFactor = consumedAmount / 100.0
                            val scaledNutrients = productToSave.nutrients?.copy(
                                caloriesPer100g = (productToSave.nutrients.caloriesPer100g ?: 0.0) * scaleFactor,
                                sugarsPer100g = (productToSave.nutrients.sugarsPer100g ?: 0.0) * scaleFactor,
                                caffeinePer100g = (productToSave.nutrients.caffeinePer100g ?: 0.0) * scaleFactor,
                                saturatedFatPer100g = (productToSave.nutrients.saturatedFatPer100g ?: 0.0) * scaleFactor
                            )

                            // Create a new product instance with a new timestamp (primary key) and scaled data
                            val finalProduct = productToSave.copy(
                                createdAt = System.currentTimeMillis(), // New unique key
                                consumedAmount = consumedAmount,
                                nutrients = scaledNutrients
                            )

                            scope.launch {
                                repo.insertDrinkProduct(finalProduct)
                            }
                        }

                        showConfirmationDialog = false
                        selectedProduct = null
                        weightInput = "100" // Reset for next use
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false
                        selectedProduct = null
                        weightInput = "100" // Reset for next use
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    fun searchAction() {
        if (productId.isBlank()) {
            return
        }

        if (uiState is SearchState.Loading) {
            Log.d(TAG, "Already loading, ignoring request")
            return
        }

        val searchTerm = parseInput(productId)
        scope.launch {
            client.searchProduct(searchTerm)
        }
        keyboardController?.hide()
    }

    if (isScanning) {
        ScanRunning(
            onBarcodeDetected = { barcode ->
                Log.d(TAG, "Barcode detected: $barcode")
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
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp)
                    .fillMaxWidth()
            ) {
                ScannerSearchBox(
                    productId = productId,
                    setProductId = { productId = it },
                    uiState = uiState,
                    onScanAction = { isScanning = true },
                    searchAction = { searchAction() }
                )

                when (val state = uiState) {
                    is SearchState.Loading -> {
                        Text(text = "Loading...", modifier = Modifier.padding(top = 16.dp))
                    }

                    is SearchState.Success -> {
                        if (state.products.isEmpty() && state.isComplete) {
                            Text(
                                text = "No products found",
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                LazyColumn {
                                    items(state.products.size) { index ->
                                        val product = state.products[index]
                                        // --- Start of modified code: Make OffCard clickable ---
                                        Box(modifier = Modifier.clickable {
                                            selectedProduct = product
                                            showConfirmationDialog = true
                                        }) {
                                            OffCard(product)
                                        }
                                        // --- End of modified code ---
                                    }
                                }

                                if (!state.isComplete) {
                                    Text(
                                        text = "Loading more results...",
                                        modifier = Modifier.padding(top = 8.dp),
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    is SearchState.Error -> {
                        Text(
                            text = "Error: ${state.reason}",
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Log.d(TAG, "Error: ${state.reason}")
                    }

                    SearchState.Init -> {
                        Text(
                            text = "Enter a product ID to search or tap camera to scan",
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScanRunning(
    onBarcodeDetected: (String) -> Unit,
    setScanning: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        CameraPreview(
            onBarcodeDetected = onBarcodeDetected,
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = setScanning,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to manual input",
                tint = Color.White
            )
        }

        Text(
            text = "Point camera at barcode to scan",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            color = Color.White
        )
    }
}
