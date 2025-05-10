package com.github.smugapp.ui.components

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@SuppressLint("MissingPermission")
@Composable
fun DeviceCard(
    device: BluetoothDevice,
    selectedDevice: BluetoothDevice? = null,
    click: (() -> Unit)? = null,
) {
    val isSelected = selectedDevice == device
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { click?.invoke() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = device.name ?: "Unknown Device",
                style = if (isSelected)
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                else
                    MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Address: ${device.address}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Type: ${getDeviceTypeName(device.type)}",
                style = MaterialTheme.typography.bodySmall
            )
            if (isSelected) {
                Text(
                    text = "✓ Selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun getDeviceTypeName(type: Int): String {
    return when (type) {
        BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
        BluetoothDevice.DEVICE_TYPE_LE -> "LE"
        BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
        BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "Unknown"
        else -> "Unknown"
    }
}