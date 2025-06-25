package com.github.smugapp.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.smugapp.data.SmugRepo
import com.github.smugapp.model.DrinkProduct
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReportViewModel(private val repo: SmugRepo) : ViewModel() {
    private val _todayDrinks = repo.getTodayDrinkProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val todayDrinks: StateFlow<List<DrinkProduct>> = _todayDrinks

    val totalCalories: StateFlow<Double> = _todayDrinks
        .map { drinks ->
            drinks.sumOf { it.nutrients?.caloriesPer100g ?: 0.0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    private val _weeklyDrinks = repo.getWeeklyDrinkProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val weeklyDrinks: StateFlow<List<DrinkProduct>> = _weeklyDrinks

    val weeklyCalories: StateFlow<Double> = _weeklyDrinks
        .map { drinks -> drinks.sumOf { it.nutrients?.caloriesPer100g ?: 0.0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    // --- NEW: Added total weekly volume calculation ---
    val totalWeeklyVolume: StateFlow<Double> = _weeklyDrinks
        .map { drinks -> drinks.sumOf { (it.consumedAmount ?: 0).toDouble() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)
    // --- End of new code ---

    // Aggregated data for charts
    val volumeByType: StateFlow<Map<String, Double>> = _weeklyDrinks
        .map { drinks -> aggregateVolumeByType(drinks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    val caloriesByType: StateFlow<Map<String, Double>> = _weeklyDrinks
        .map { drinks -> aggregateCaloriesByType(drinks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    val dailyVolumeByType: StateFlow<Map<String, Map<String, Double>>> = _weeklyDrinks
        .map { drinks -> aggregateDailyVolumeByType(drinks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    val dailyCaloriesByType: StateFlow<Map<String, Map<String, Double>>> = _weeklyDrinks
        .map { drinks -> aggregateDailyCaloriesByType(drinks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    val dailyWaterIntake: StateFlow<Map<String, Double>> = _weeklyDrinks
        .map { drinks -> aggregateDailyWaterIntake(drinks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    fun deleteDrinkProduct(drinkProduct: DrinkProduct) {
        viewModelScope.launch {
            repo.deleteDrinkProduct(drinkProduct)
        }
    }

    fun deleteAllEntries() {
        viewModelScope.launch {
            repo.deleteAllDrinkProducts()
        }
    }

    private fun toDateString(timestamp: Long): String {
        val formatter = SimpleDateFormat("MM-dd", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    private fun aggregateVolumeByType(drinks: List<DrinkProduct>): Map<String, Double> {
        return drinks.groupBy { it.getSensibleName() }
            .mapValues { (_, drinkList) ->
                drinkList.sumOf { (it.consumedAmount ?: 100).toDouble() }
            }
    }

    private fun aggregateCaloriesByType(drinks: List<DrinkProduct>): Map<String, Double> {
        return drinks.groupBy { it.getSensibleName() }
            .mapValues { (_, drinkList) ->
                drinkList.sumOf { drink ->
                    drink.nutrients?.caloriesPer100g ?: 0.0
                }
            }
    }

    private fun aggregateDailyVolumeByType(drinks: List<DrinkProduct>): Map<String, Map<String, Double>> {
        return drinks.groupBy { toDateString(it.createdAt) }
            .mapValues { (_, dailyDrinks) ->
                dailyDrinks.groupBy { it.getSensibleName() }
                    .mapValues { (_, drinkList) ->
                        drinkList.sumOf { (it.consumedAmount ?: 100).toDouble() }
                    }
            }
    }

    private fun aggregateDailyCaloriesByType(drinks: List<DrinkProduct>): Map<String, Map<String, Double>> {
        return drinks.groupBy { toDateString(it.createdAt) }
            .mapValues { (_, dailyDrinks) ->
                dailyDrinks.groupBy { it.getSensibleName() }
                    .mapValues { (_, drinkList) ->
                        drinkList.sumOf { drink ->
                            drink.nutrients?.caloriesPer100g ?: 0.0
                        }
                    }
            }
    }

    private fun aggregateDailyWaterIntake(drinks: List<DrinkProduct>): Map<String, Double> {
        return drinks.filter { it.getSensibleName().lowercase().contains("water") }
            .groupBy { toDateString(it.createdAt) }
            .mapValues { (_, waterDrinks) ->
                waterDrinks.sumOf { (it.consumedAmount ?: 100).toDouble() }
            }
    }
}
