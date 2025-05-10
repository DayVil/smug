package com.github.smugapp.network.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.github.smugapp.network.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

private const val TAG = "BluetoothLEConnectionHandler"


class BluetoothLEConnectionHandler(private val context: Context) {
    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _discoveredServices = MutableStateFlow<List<BluetoothGattService>>(emptyList())
    val discoveredServices: StateFlow<List<BluetoothGattService>> = _discoveredServices

    private val characteristicListeners = mutableMapOf<UUID, (ByteArray) -> Unit>()

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.i(TAG, "Successfully connected to $deviceAddress")
                        _connectionState.value = ConnectionState.CONNECTED

                        // Discover services after connecting
                        Log.i(TAG, "Attempting to discover services on $deviceAddress")
                        gatt.discoverServices()
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i(TAG, "Successfully disconnected from $deviceAddress")
                        _connectionState.value = ConnectionState.DISCONNECTED
                        closeGatt()
                    }

                    BluetoothProfile.STATE_CONNECTING -> {
                        Log.i(TAG, "Connecting to $deviceAddress")
                        _connectionState.value = ConnectionState.CONNECTING
                    }

                    BluetoothProfile.STATE_DISCONNECTING -> {
                        Log.i(TAG, "Disconnecting from $deviceAddress")
                        _connectionState.value = ConnectionState.DISCONNECTING
                    }
                }
            } else {
                Log.w(
                    TAG,
                    "Connection state change error for device $deviceAddress, status: $status"
                )
                _connectionState.value = ConnectionState.DISCONNECTED
                closeGatt()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered for ${gatt.device.address}")
                val services = gatt.services
                _discoveredServices.value = services

                // Log discovered services and characteristics
                for (service in services) {
                    Log.d(TAG, "Service discovered: ${service.uuid}")
                    for (characteristic in service.characteristics) {
                        Log.d(TAG, "  Characteristic: ${characteristic.uuid}")
                    }
                }
            } else {
                Log.w(TAG, "Service discovery failed for ${gatt.device.address}, status: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Characteristic read successful: ${characteristic.uuid}")
                Log.d(TAG, "Read value: ${value.joinToString(", ") { String.format("%02X", it) }}")
                onCharacteristicUpdated(characteristic, value)
            } else {
                Log.w(TAG, "Characteristic read failed: ${characteristic.uuid}, status: $status")
            }
        }

        // For backward compatibility with older Android versions
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "Characteristic read successful: ${characteristic.uuid}")
                    @Suppress("DEPRECATION")
                    val value = characteristic.value
                    Log.d(
                        TAG,
                        "Read value: ${value.joinToString(", ") { String.format("%02X", it) }}"
                    )
                    onCharacteristicUpdated(characteristic, value)
                } else {
                    Log.w(
                        TAG,
                        "Characteristic read failed: ${characteristic.uuid}, status: $status"
                    )
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Characteristic write successful: ${characteristic.uuid}")
            } else {
                Log.w(TAG, "Characteristic write failed: ${characteristic.uuid}, status: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.i(TAG, "Characteristic changed: ${characteristic.uuid}")
            Log.d(TAG, "New value: ${value.joinToString(", ") { String.format("%02X", it) }}")
            onCharacteristicUpdated(characteristic, value)
        }

        // For backward compatibility with older Android versions
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Log.i(TAG, "Characteristic changed: ${characteristic.uuid}")
                @Suppress("DEPRECATION")
                val value = characteristic.value
                Log.d(TAG, "New value: ${value.joinToString(", ") { String.format("%02X", it) }}")
                onCharacteristicUpdated(characteristic, value)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Descriptor write successful: ${descriptor.uuid}")
            } else {
                Log.w(TAG, "Descriptor write failed: ${descriptor.uuid}, status: $status")
            }
        }
    }

    private fun onCharacteristicUpdated(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        characteristicListeners[characteristic.uuid]?.invoke(value)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice): Boolean {
        Log.d(TAG, "Attempting to connect to device: ${device.address}")

        if (_connectionState.value == ConnectionState.CONNECTED) {
            Log.w(TAG, "Already connected to a device. Disconnect first.")
            return false
        }

        if (_connectionState.value == ConnectionState.CONNECTING) {
            Log.w(TAG, "Already connecting to a device. Wait for connection to complete.")
            return false
        }

        _connectionState.value = ConnectionState.CONNECTING

        // Create a new GATT connection
        bluetoothGatt = device.connectGatt(context, true, gattCallback)

        return bluetoothGatt != null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun closeGatt() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        _discoveredServices.value = emptyList()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun setCharacteristicNotification(
        serviceUuid: UUID,
        characteristicUuid: UUID,
        enable: Boolean,
        listener: ((ByteArray) -> Unit)? = null
    ): Boolean {
        if (bluetoothGatt == null) {
            Log.e(TAG, "bluetoothGatt is null when trying to set notification")
            return false
        }
        Log.d(TAG, "Attempting to get service: $serviceUuid")
        Log.d(TAG, "Current connection state: ${_connectionState.value}")
        Log.d(TAG, "Available services: ${discoveredServices.value.map { it.uuid }}")

        // Ensure we're in a connected state
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.e(TAG, "Not connected when trying to access service: $serviceUuid")
            return false
        }

        // Get the service
        val service = bluetoothGatt?.getService(serviceUuid)
        if (service == null) {
            Log.e(TAG, "Service not found: $serviceUuid")
            return false
        }

        // Get the characteristic
        val characteristic = service.getCharacteristic(characteristicUuid) ?: run {
            Log.e(TAG, "Characteristic not found: $characteristicUuid")
            return false
        }

        // Set or remove the listener
        if (enable && listener != null) {
            characteristicListeners[characteristicUuid] = listener
        } else {
            characteristicListeners.remove(characteristicUuid)
        }

        // Enable/disable notification
        val success = bluetoothGatt?.setCharacteristicNotification(characteristic, enable) ?: false

        if (!success) {
            Log.e(TAG, "Failed to set notification for: $characteristicUuid")
            return false
        }

        val descriptor = characteristic.getDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") // Standard CCCD UUID
        ) ?: run {
            Log.e(TAG, "CCCD not found for: $characteristicUuid")
            Log.d(
                TAG,
                "Attempting to use notifications without CCCD - this may work on some devices"
            )
            return true
        }

        val descriptorValue = if (enable) {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }

        Log.d(TAG, "Writing descriptor for characteristic: ${characteristic.uuid}")

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Just call the method and consider that success is any non-negative result
            val result = bluetoothGatt?.writeDescriptor(descriptor, descriptorValue)
            Log.d(TAG, "Descriptor write request result: $result")
            result != null // Just verify the request was accepted
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = descriptorValue

            @Suppress("DEPRECATION")
            val result = bluetoothGatt?.writeDescriptor(descriptor)
            Log.d(TAG, "Descriptor write result: $result")
            result == true
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED &&
                bluetoothGatt != null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun ensureServicesDiscovered(): Boolean {
        if (!isConnected()) {
            Log.e(TAG, "Cannot discover services: Not connected")
            return false
        }

        if (_discoveredServices.value.isEmpty()) {
            Log.d(TAG, "Attempting to re-discover services")
            return bluetoothGatt?.discoverServices() == true
        }

        return true
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun cleanup() {
        Log.d(TAG, "Cleaning up BLE connection")
        characteristicListeners.clear()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
} 