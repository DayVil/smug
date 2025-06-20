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
            .padding(vertical = 4.dp),
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
                    val maxLen = 20
                    Text(text = "Product name:")
                    var sensibleName = product.getSensibleName()
                    if (sensibleName.length > maxLen) {
                        sensibleName = sensibleName.substring(0, maxLen) + "..."
                    }
                    Text(text = sensibleName)
                    Text(text = "Brands:", modifier = Modifier.padding(top = 10.dp))
                    var brands = product.brands ?: "Unknown"
                    if (brands.length > maxLen) {
                        brands = brands.substring(0, maxLen) + "..."
                    }
                    Text(text = brands)
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