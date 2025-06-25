package com.github.smugapp.ui.screens.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.smugapp.model.DrinkProduct
import com.github.smugapp.ui.components.PieChart
import com.github.smugapp.ui.components.SimpleBarChart
import com.github.smugapp.ui.components.StackedBarChart
import kotlinx.serialization.Serializable

@Serializable
object ReportScreenRoute

@Composable
fun ReportScreen(viewModel: ReportViewModel) {
    var showWeekly by rememberSaveable { mutableStateOf(true) } // Initial state set to weekly
    var showCharts by rememberSaveable { mutableStateOf(false) }

    // Conditionally collect state based on the 'showWeekly' toggle
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

    // Collect chart data and total volume for chart titles
    val totalWeeklyVolume by viewModel.totalWeeklyVolume.collectAsState()
    val volumeByType by viewModel.volumeByType.collectAsState()
    val caloriesByType by viewModel.caloriesByType.collectAsState()
    val dailyVolumeByType by viewModel.dailyVolumeByType.collectAsState()
    val dailyCaloriesByType by viewModel.dailyCaloriesByType.collectAsState()
    val dailyWaterIntake by viewModel.dailyWaterIntake.collectAsState()

    // State for the single item deletion confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var drinkToDelete by remember { mutableStateOf<DrinkProduct?>(null) }

    // State for the "Delete All" confirmation dialog
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }

    // Single item deletion confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Beverage") },
            text = { Text("Are you sure you want to delete this beverage entry?") },
            confirmButton = {
                Button(onClick = {
                    drinkToDelete?.let { viewModel.deleteDrinkProduct(it) }
                    showDeleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // "Delete All" confirmation dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Entries?") },
            text = { Text("Are you sure you want to permanently delete all entries? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllEntries()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Confirm Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            }
        )
    }

    // State to toggle mini-charts inside each drink card
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
        // Header with title and toggle buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showCharts) "Weekly Charts" else if (showWeekly) "Weekly Drinks" else "Today's Drinks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // "Delete All" button - visible only if there are drinks
                if (drinks.isNotEmpty()) {
                    IconButton(onClick = { showDeleteAllDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever, // Ensure this icon is imported
                            contentDescription = "Delete All Entries",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                IconButton(onClick = { showCharts = !showCharts }) {
                    Icon(
                        imageVector = if (showCharts) Icons.Default.List else Icons.Default.BarChart,
                        contentDescription = "Toggle Charts View"
                    )
                }

                // Only show the day/week toggle when not in chart view
                if (!showCharts) {
                    IconButton(onClick = { showWeekly = !showWeekly }) {
                        Icon(
                            imageVector = if (showWeekly) Icons.Default.CalendarMonth else Icons.Default.Today,
                            contentDescription = "Toggle Weekly/Daily View"
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Conditional content: show charts or the drink list
        if (showCharts) {
            item {
                ChartCard(title = "Volume Distribution (Total: ${totalWeeklyVolume.toInt()} ml)") {
                    PieChart(data = volumeByType, modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                ChartCard(title = "Calories Distribution (Total: ${totalCalories.toInt()} kcal)") {
                    PieChart(data = caloriesByType, modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                ChartCard(title = "Daily Volume by Type (Total: ${totalWeeklyVolume.toInt()} ml)") {
                    StackedBarChart(
                        data = dailyVolumeByType,
                        modifier = Modifier.fillMaxWidth(),
                        yAxisUnit = "ml" // Pass unit for y-axis
                    )
                }
            }
            item {
                ChartCard(title = "Daily Calories by Type (Total: ${totalCalories.toInt()} kcal)") {
                    StackedBarChart(
                        data = dailyCaloriesByType,
                        modifier = Modifier.fillMaxWidth(),
                        yAxisUnit = "kcal" // Pass unit for y-axis
                    )
                }
            }
            item {
                ChartCard(title = "Daily Water Intake") {
                    SimpleBarChart(
                        data = dailyWaterIntake,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else {
            // Display the list of drink entries
            items(drinks, key = { it.createdAt }) { drink ->
                DrinkItemCard(
                    drink = drink,
                    onDelete = {
                        drinkToDelete = it
                        showDeleteDialog = true
                    },
                    onToggleChart = {
                        chartStates[it.createdAt] = !(chartStates[it.createdAt] ?: false)
                    },
                    isChartVisible = chartStates[drink.createdAt] == true
                )
            }
        }

        // Footer with total calories summary (always visible)
        item {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Text(
                text = "Total Calories: ${totalCalories.toInt()} kcal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * A reusable Card composable for displaying charts with a title.
 */
@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

/**
 * A Card composable for displaying a single drink entry.
 */
@Composable
private fun DrinkItemCard(
    drink: DrinkProduct,
    onDelete: (DrinkProduct) -> Unit,
    onToggleChart: (DrinkProduct) -> Unit,
    isChartVisible: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val kcal = drink.nutrients?.caloriesPer100g?.toFloat() ?: 0f
        val sugar = drink.nutrients?.sugarsPer100g?.toFloat() ?: 0f
        val caffeine = drink.nutrients?.caffeinePer100g?.toFloat() ?: 0f
        val fat = drink.nutrients?.saturatedFatPer100g?.toFloat() ?: 0f

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = drink.getSensibleName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Amount: ${drink.consumedAmount ?: 100} g/ml",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = { onDelete(drink) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete beverage",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = { onToggleChart(drink) }) {
                        Icon(
                            imageVector = Icons.Default.AddChart,
                            contentDescription = "Show nutrient chart",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (isChartVisible) {
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
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    NutrientItem(label = "Kalorien", value = "${kcal.toInt()} kcal")
                    NutrientItem(label = "Zucker", value = "%.1fg".format(sugar))
                    NutrientItem(label = "Koffein", value = "%.1fg".format(caffeine))
                }
            }
        }
    }
}

/**
 * A small composable to display a nutrient label and its value.
 */
@Composable
fun NutrientItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

/**
 * A simple bar chart for showing the nutrients of a single drink.
 */
@Composable
fun NutrientChart(nutrients: Map<String, Float>) {
    val max = nutrients.values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    val barWidth = 40.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        nutrients.forEach { (label, value) ->
            if (value > 0) { // Only show bars for non-zero values
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "%.1f".format(value),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(120.dp * (value / max)) // Corrected calculation
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
