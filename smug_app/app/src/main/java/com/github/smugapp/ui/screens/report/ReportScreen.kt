package com.github.smugapp.ui.screens.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.github.smugapp.model.DrinkProduct
import kotlinx.serialization.Serializable

@Serializable
object ReportScreenRoute

@Composable
fun ReportScreen(viewModel: ReportViewModel) {
    var showWeekly by rememberSaveable { mutableStateOf(false) }

    val drinks by if (showWeekly) {
        viewModel.weeklyDrinks.collectAsState()
    } else {
        viewModel.todayDrinks.collectAsState()
    }

    val totalCalories by if (showWeekly) {
        viewModel.weeklyCalories.collectAsState()
    } else {
        viewModel.totalCalories.collectAsState()
    }

    // State for deletion
    var showDeleteDialog by remember { mutableStateOf(false) }
    var drinkToDelete by remember { mutableStateOf<DrinkProduct?>(null) }

    // Deletion confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Beverage") },
            text = { Text("Are you sure you want to delete this beverage entry?") },
            confirmButton = {
                Button(
                    onClick = {
                        drinkToDelete?.let { viewModel.deleteDrinkProduct(it) }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // A bug in compose requires a non-primitive key for mutableStateMapOf to recompose correctly
    // Using the timestamp, which is our primary key, is a reliable choice.
    val chartStates = remember(drinks) {
        mutableStateMapOf<Long, Boolean>().apply {
            drinks.forEach { put(it.createdAt, false) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showWeekly) "Weekly Drinks" else "Today's Drinks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { showWeekly = !showWeekly }) {
                    Icon(
                        imageVector = if (showWeekly) Icons.Default.FilterAltOff else Icons.Default.BarChart,
                        contentDescription = "Toggle View"
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(drinks, key = { it.createdAt }) { drink ->
            val kcal = drink.nutrients?.caloriesPer100g?.toFloat() ?: 0f
            val sugar = drink.nutrients?.sugarsPer100g?.toFloat() ?: 0f
            val caffeine = drink.nutrients?.caffeinePer100g?.toFloat() ?: 0f
            val fat = drink.nutrients?.saturatedFatPer100g?.toFloat() ?: 0f

            val showChart = chartStates[drink.createdAt] == true

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFB3E5FC))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // --- Start of Modified Section ---
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = drink.getSensibleName(), // Using the helper function
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // This new Text composable displays the consumed amount
                            Text(
                                text = "Amount: ${drink.consumedAmount ?: 100} g/ml",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // --- End of Modified Section ---

                        // Action buttons row
                        Row {
                            // Delete button
                            IconButton(
                                onClick = {
                                    drinkToDelete = drink
                                    showDeleteDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete beverage",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }

                            // Chart toggle button
                            IconButton(
                                onClick = {
                                    chartStates[drink.createdAt] = !(chartStates[drink.createdAt] ?: false)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddChart,
                                    contentDescription = "Show nutrient chart",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (showChart) {
                        NutrientChart(
                            nutrients = mapOf(
                                "Kalorien" to kcal,
                                "Zucker" to sugar,
                                "Koffein" to caffeine,
                                "Fett" to fat
                            )
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NutrientItem(label = "Kalorien", value = "${kcal.toInt()} kcal")
                            NutrientItem(label = "Zucker", value = "%.1fg".format(sugar))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NutrientItem(label = "Koffein", value = "%.1fg".format(caffeine))
                            NutrientItem(label = "Fett", value = "%.1fg".format(fat))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Text(
                text = "Gesamtkalorien: ${totalCalories.toInt()} kcal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 8.dp)
            )
        }
    }
}


@Composable
fun NutrientItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun NutrientChart(nutrients: Map<String, Float>) {
    val max = nutrients.values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    val barWidth = 40.dp
    val spacing = 12.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.Bottom
    ) {
        nutrients.forEach { (label, value) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .height((value / max) * 120.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(6.dp)
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "%.1f".format(value),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
