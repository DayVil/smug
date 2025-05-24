package com.github.smugapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.smugapp.model.DrinkProduct

@Composable
fun OffCard(product: DrinkProduct) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Box(
            modifier = Modifier.padding(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Product name:")
                    Text(text = product.getSensibleName())
                }

                Column {
                    Text(text = "Nutrients per 100g:")
                    Text(text = "Calories: ${product.nutrients?.caloriesPer100g ?: 0} kcal")
                    Text(text = "Fat: ${product.nutrients?.saturatedFatPer100g ?: 0} g")
                    Text(text = "Sugars: ${product.nutrients?.sugarsPer100g ?: 0} g")
                    Text(text = "Caffeine: ${product.nutrients?.caffeinePer100g ?: 0} mg")
                }
            }
        }
    }
}