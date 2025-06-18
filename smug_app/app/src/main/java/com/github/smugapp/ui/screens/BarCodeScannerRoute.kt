package com.github.smugapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.smugapp.data.DrinkRepo
import com.github.smugapp.model.DrinkProduct
import com.github.smugapp.network.OffService
import com.github.smugapp.ui.components.CameraPreview
import com.github.smugapp.ui.components.OffCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

private const val TAG = "BarCodeScanner"

@Serializable
object BarCodeScannerRoute

sealed interface UiState {
    data object Init : UiState
    data object Loading : UiState
    data class Success(val data: DrinkProduct) : UiState
    data class Error(val exception: Throwable) : UiState
}

@Composable
fun BarCodeScannerContent(repo: DrinkRepo) {
    var uiState by remember { mutableStateOf<UiState>(UiState.Init) }
    var productId by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val client = remember { OffService(repo) }
    val keyboardController = LocalSoftwareKeyboardController.current

    DisposableEffect(Unit) {
        onDispose {
            client.close()
            Log.d(TAG, "Client closed")
        }
    }

    fun searchAction() {
        if (productId.isBlank()) {
            return
        }

        if (uiState is UiState.Loading) {
            Log.d(TAG, "Already loading, ignoring request")
            return
        }

        Log.d(TAG, "Running request for ID: $productId")
        uiState = UiState.Loading
        scope.launch {
            val product = client.fetchProduct(productId)
            uiState = product.fold(
                onSuccess = {
                    Log.d(TAG, "Success: $it")
                    UiState.Success(it)
                },
                onFailure = {
                    Log.d(TAG, "Failure: $it")
                    UiState.Error(it)
                }
            )
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

                OutlinedTextField(
                    value = productId,
                    onValueChange = { productId = it },
                    label = { Text("Product ID") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            searchAction()
                        }
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { searchAction() },
                            enabled = productId.isNotBlank() && uiState !is UiState.Loading
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search"
                            )
                        }
                    },
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                isScanning = true
                            },
                            enabled = uiState !is UiState.Loading
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = "Scan barcode"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState !is UiState.Loading
                )
                when (val state = uiState) {
                    is UiState.Loading -> {
                        Text(text = "Loading...", modifier = Modifier.padding(top = 16.dp))
                    }

                    is UiState.Success -> {
                        OffCard(state.data)
                    }

                    is UiState.Error -> {
                        Text(
                            text = "Error: ${state.exception}",
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Log.d(TAG, "Error: ${state.exception}")
                    }

                    UiState.Init -> {
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