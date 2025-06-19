package com.github.smugapp.ui.screens.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

@Serializable
object ReportScreenRoute

@Composable
fun ReportScreen(viewModel: ReportViewModel) {
    val drinks by viewModel.todayDrinks.collectAsState()
    val totalCalories by viewModel.totalCalories.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            Text("Today's Drinks", style = MaterialTheme.typography.headlineSmall)

//            Icon(imageVector = Icons.Filled.FilterAlt,
//                contentDescription = "Filter"
//            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        drinks.forEach { drink ->
            val kcal = drink.nutrients?.caloriesPer100g ?: "?"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(drink.getSensibleName())
                Text("$kcal kcal")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Total Calories: $totalCalories kcal",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
