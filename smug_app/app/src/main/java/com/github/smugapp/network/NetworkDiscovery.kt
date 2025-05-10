package com.github.smugapp.network

interface NetworkDiscovery {
    fun startScan(): Boolean
    fun stopScan()
    fun cleanup()
}