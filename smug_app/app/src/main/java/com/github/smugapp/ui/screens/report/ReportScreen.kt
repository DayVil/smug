package com.github.smugapp.ui.screens.report

import android.text.method.ScrollingMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.github.smugapp.model.DrinkProduct
import com.github.smugapp.ui.components.PieChart
import com.github.smugapp.ui.components.SimpleBarChart
import com.github.smugapp.ui.components.StackedBarChart
import com.github.smugapp.ui.theme.BlueWater
import kotlinx.serialization.Serializable

@Serializable
object ReportScreenRoute

@Composable
fun ReportScreen(viewModel: ReportViewModel) {
    var showWeekly by rememberSaveable { mutableStateOf(true) }
    var showCharts by rememberSaveable { mutableStateOf(false) }
    // --- UPDATED: Changed from weekly to daily goal ---
    var dailyGoal by rememberSaveable { mutableStateOf("2000") }

    val drinks by if (showWeekly) viewModel.weeklyDrinks.collectAsState() else viewModel.todayDrinks.collectAsState()
    val totalCalories by if (showWeekly) viewModel.weeklyCalories.collectAsState() else viewModel.totalCalories.collectAsState()

    // --- UPDATED: Collect both daily and weekly volumes ---
    val totalWeeklyVolume by viewModel.totalWeeklyVolume.collectAsState()
    val totalDailyVolume by viewModel.totalDailyVolume.collectAsState()

    val volumeByType by viewModel.volumeByType.collectAsState()
    val caloriesByType by viewModel.caloriesByType.collectAsState()
    val dailyVolumeByType by viewModel.dailyVolumeByType.collectAsState()
    val dailyCaloriesByType by viewModel.dailyCaloriesByType.collectAsState()
    val dailyWaterIntake by viewModel.dailyWaterIntake.collectAsState()
    val insightsState by viewModel.insightsState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var drinkToDelete by remember { mutableStateOf<DrinkProduct?>(null) }
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var drinkToEdit by remember { mutableStateOf<DrinkProduct?>(null) }
    var editAmount by remember { mutableStateOf("") }

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
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Entries?") },
            text = { Text("Are you sure? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = { viewModel.deleteAllEntries(); showDeleteAllDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Confirm Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") } }
        )
    }

    if (showEditDialog && drinkToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Amount") },
            text = {
                Column {
                    Text("Enter new amount for '${drinkToEdit?.getSensibleName()}' (in g/ml):")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { newValue -> editAmount = newValue.filter { it.isDigit() } },
                        label = { Text("Amount (g/ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAmount = editAmount.toIntOrNull()
                        if (newAmount != null && newAmount > 0) {
                            drinkToEdit?.let { viewModel.updateDrinkAmount(it, newAmount) }
                        }
                        showEditDialog = false
                        editAmount = ""
                    },
                    enabled = editAmount.isNotBlank() && editAmount.toIntOrNull() != null
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } }
        )
    }

    if (insightsState !is InsightsState.Idle) {
        InsightsDialog(state = insightsState, onDismiss = { viewModel.resetInsightsState() })
    }

    val chartStates = remember(drinks) {
        mutableStateMapOf<Long, Boolean>().apply {
            drinks.forEach { put(it.createdAt, false) }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (showCharts) "Weekly Charts" else if (showWeekly) "Weekly Drinks" else "Today's Drinks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (drinks.isNotEmpty()) {
                    IconButton(onClick = { viewModel.getDrinkingInsights() }) { Icon(Icons.Default.Lightbulb, "Get Insights") }
                    IconButton(onClick = { showDeleteAllDialog = true }) { Icon(Icons.Default.DeleteForever, "Delete All Entries", tint = MaterialTheme.colorScheme.error) }
                }
                IconButton(onClick = { showCharts = !showCharts }) { Icon(if (showCharts) Icons.Default.List else Icons.Default.BarChart, "Toggle Charts View") }
                if (!showCharts) {
                    IconButton(onClick = { showWeekly = !showWeekly }) { Icon(if (showWeekly) Icons.Default.CalendarMonth else Icons.Default.Today, "Toggle Weekly/Daily View") }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // --- UPDATED: Show daily goal only on the daily view ---
        if (!showWeekly) {
            item {
                DailyGoalSection(
                    currentVolume = totalDailyVolume,
                    goal = dailyGoal,
                    onGoalChange = { dailyGoal = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showCharts) {
            item { ChartCard(title = "Volume Distribution (Total: ${totalWeeklyVolume.toInt()} ml)") { PieChart(data = volumeByType) } }
            item { ChartCard(title = "Calories Distribution (Total: ${totalCalories.toInt()} kcal)") { PieChart(data = caloriesByType) } }
            item { ChartCard(title = "Daily Volume by Type (Total: ${totalWeeklyVolume.toInt()} ml)") { StackedBarChart(data = dailyVolumeByType, yAxisUnit = "ml") } }
            item { ChartCard(title = "Daily Calories by Type (Total: ${totalCalories.toInt()} kcal)") { StackedBarChart(data = dailyCaloriesByType, yAxisUnit = "kcal") } }
            item { ChartCard(title = "Daily Water Intake") { SimpleBarChart(data = dailyWaterIntake, color = BlueWater) } }
        } else {
            items(drinks, key = { it.createdAt }) { drink ->
                DrinkItemCard(
                    drink = drink,
                    onDelete = { drinkToDelete = it; showDeleteDialog = true },
                    onEdit = { drinkToEdit = it; editAmount = it.consumedAmount.toString(); showEditDialog = true },
                    onToggleChart = { chartStates[it.createdAt] = !(chartStates[it.createdAt] ?: false) },
                    isChartVisible = chartStates[drink.createdAt] == true
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Text(
                "Total Calories: ${totalCalories.toInt()} kcal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// --- RENAMED and UPDATED: Composable for the daily goal section ---
@Composable
private fun DailyGoalSection(currentVolume: Double, goal: String, onGoalChange: (String) -> Unit) {
    val goalFloat = goal.toFloatOrNull() ?: 0f
    val progress = if (goalFloat > 0) (currentVolume.toFloat() / goalFloat).coerceIn(0f, 1f) else 0f
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Daily Liquid Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = goal,
                    onValueChange = { onGoalChange(it.filter { c -> c.isDigit() }) },
                    label = { Text("Goal (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${currentVolume.toInt()} / ${goalFloat.toInt()} ml",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun InsightsDialog(state: InsightsState, onDismiss: () -> Unit) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val bodyMediumFontSize = MaterialTheme.typography.bodyMedium.fontSize
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your Weekly Insights") },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                when (state) {
                    is InsightsState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is InsightsState.Success -> {
                        AndroidView(
                            factory = { context -> TextView(context).apply { isVerticalScrollBarEnabled = true; movementMethod = ScrollingMovementMethod(); setTextIsSelectable(true) } },
                            update = { view ->
                                val htmlText = state.text
                                    .replace(Regex("\\*\\*(.*?)\\*\\*")) { "<b>${it.groupValues[1]}</b>" }
                                    .lines()
                                    .joinToString("<br>") { line ->
                                        val trimmedLine = line.trim()
                                        if (trimmedLine.startsWith("* ")) "&#8226; ${trimmedLine.substring(2)}" else line
                                    }
                                view.text = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_COMPACT)
                                view.setTextColor(onSurfaceColor.toArgb())
                                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, bodyMediumFontSize.value)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is InsightsState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.error) }
                    is InsightsState.Idle -> {}
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            content()
        }
    }
}

@Composable
private fun DrinkItemCard(drink: DrinkProduct, onDelete: (DrinkProduct) -> Unit, onEdit: (DrinkProduct) -> Unit, onToggleChart: (DrinkProduct) -> Unit, isChartVisible: Boolean) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) {
        val kcal = drink.nutrients?.caloriesPer100g?.toFloat() ?: 0f
        val sugar = drink.nutrients?.sugarsPer100g?.toFloat() ?: 0f
        val caffeine = drink.nutrients?.caffeinePer100g?.toFloat() ?: 0f
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(drink.getSensibleName(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Amount: ${drink.consumedAmount ?: 100} g/ml", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(onClick = { onEdit(drink) }) { Icon(Icons.Default.Edit, "Edit amount", tint = MaterialTheme.colorScheme.secondary) }
                    IconButton(onClick = { onDelete(drink) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                    IconButton(onClick = { onToggleChart(drink) }) { Icon(Icons.Default.AddChart, "Chart", tint = MaterialTheme.colorScheme.primary) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (isChartVisible) {
                NutrientChart(mapOf("Kalorien" to kcal, "Zucker" to sugar))
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    NutrientItem("Kalorien", "${kcal.toInt()} kcal")
                    NutrientItem("Zucker", "%.1fg".format(sugar))
                }
            }
        }
    }
}

@Composable
fun NutrientItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun NutrientChart(nutrients: Map<String, Float>) {
    val max = nutrients.values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    Row(modifier = Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        nutrients.forEach { (label, value) ->
            if (value > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text("%.1f".format(value),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Clip)
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier.width(40.dp).height(100.dp * (value / max)).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp, 6.dp)))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}