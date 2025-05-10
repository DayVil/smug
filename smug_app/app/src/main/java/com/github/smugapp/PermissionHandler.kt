package com.github.smugapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

const val TAG = "MainActivity"

private fun requestPermissionLauncher(context: ComponentActivity): ActivityResultLauncher<Array<String>> {
    return context.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        Log.d(TAG, "Permission request result: ${if (allGranted) "all granted" else "some denied"}")
        if (!allGranted) {
            Log.w(TAG, "Some permissions were denied. Bluetooth functionality may be limited.")
            permissions.entries.forEach { (permission, granted) ->
                if (!granted) {
                    Log.w(TAG, "Permission denied: $permission")
                }
            }
        }
    }
}

fun fetchPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}

fun checkAndRequestPermissions(context: ComponentActivity) {
    Log.d(TAG, "Checking and requesting permissions")
    val permissions = fetchPermissions()

    val permissionsToRequest = permissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }.toTypedArray()

    if (permissionsToRequest.isNotEmpty()) {
        Log.d(TAG, "Requesting permissions: ${permissionsToRequest.joinToString()}")
        requestPermissionLauncher(context).launch(permissionsToRequest)
    } else {
        Log.d(TAG, "All required permissions are already granted")
    }
}

fun checkPermissions(permissions: Array<String>, context: Context): Boolean {
    Log.d(TAG, "Checking required permissions")
    val missingPermissions = permissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    if (missingPermissions.isNotEmpty()) {
        Log.e(TAG, "Missing permissions: ${missingPermissions.joinToString()}")
        return false
    }

    Log.d(TAG, "All required permissions are granted")
    return true
}

fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    Log.d(TAG, "Location services - GPS: $isGpsEnabled, Network: $isNetworkEnabled")

    if (!isGpsEnabled && !isNetworkEnabled) {
        Log.d(TAG, "Location services are disabled")
        // Open location settings
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        context.startActivity(intent)
        return false
    }
    return true
}