package com.github.smugapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable

@Serializable
object MainScreenRoute

@Composable
fun MainScreenContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to Smug!", fontSize = 24.sp)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { /* TODO: Navigate to drink selection */ }) {
            Text("Select Liquid")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { /* TODO: Manual entry popup */ }) {
            Text("+ Add Manual Entry")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Orange Juice", fontSize = 20.sp)
        Text(text = "Weight: 170 ml", fontSize = 16.sp)
        Text(text = "Calories: 70 kcal", fontSize = 16.sp)

        Spacer(modifier = Modifier.height(32.dp))

        Text("Daily Total: 550 ml | 220 kcal")
    }
}
