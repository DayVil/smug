package com.github.smugapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.github.smugapp.ui.screens.UiState

private const val TAG = "BarCodeScanner"

@Composable
fun ScannerSearchBox(
    productId: String,
    setProductId: (String) -> Unit,
    uiState: UiState,
    onScanAction: () -> Unit,
    searchAction: () -> Unit
) {
    OutlinedTextField(
        value = productId,
        onValueChange = setProductId,
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
                onClick = onScanAction,
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
}

