package com.github.smugapp.ui.screens.report

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.smugapp.data.SmugRepo
import com.github.smugapp.model.DrinkProduct
import com.github.smugapp.network.GeminiApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// (InsightsState sealed class remains the same)
sealed class InsightsState {
    object Idle : InsightsState()
    object Loading : InsightsState()
    data class Success(val text: String) : InsightsState()
    data class Error(val message: String) : InsightsState()
}


class ReportViewModel(
    private val repo: SmugRepo,
    private val geminiService: GeminiApiService
) : ViewModel() {

    // (Your existing StateFlows and other code remain the same)
    private val _insightsState = MutableStateFlow<InsightsState>(InsightsState.Idle)
    val insightsState: StateFlow<InsightsState> = _insightsState.asStateFlow()

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

    val totalWeeklyVolume: StateFlow<Double> = _weeklyDrinks
        .map { drinks -> drinks.sumOf { (it.consumedAmount ?: 0).toDouble() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

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

    val totalDailyVolume: StateFlow<Double> = _todayDrinks
        .map { drinks -> drinks.sumOf { (it.consumedAmount ?: 0).toDouble() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    // --- START OF FIX ---
    fun updateDrinkAmount(drink: DrinkProduct, newAmount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldAmount = drink.consumedAmount ?: 100
            // Avoid division by zero if the old amount was somehow 0
            if (oldAmount == 0) return@launch

            // Calculate the factor to scale the nutrients by
            val scaleFactor = newAmount.toDouble() / oldAmount.toDouble()

            // Apply the scaling factor to the existing nutrient values
            val updatedNutrients = drink.nutrients?.copy(
                caloriesPer100g = (drink.nutrients.caloriesPer100g ?: 0.0) * scaleFactor,
                sugarsPer100g = (drink.nutrients.sugarsPer100g ?: 0.0) * scaleFactor,
                caffeinePer100g = (drink.nutrients.caffeinePer100g ?: 0.0) * scaleFactor,
                saturatedFatPer100g = (drink.nutrients.saturatedFatPer100g ?: 0.0) * scaleFactor
            )

            // Create the final updated drink object with both new amount and new nutrients
            val updatedDrink = drink.copy(
                consumedAmount = newAmount,
                nutrients = updatedNutrients
            )

            // Save the fully updated object to the database
            repo.updateDrinkProduct(updatedDrink)
        }
    }
    // --- END OF FIX ---


    fun getDrinkingInsights() {
        viewModelScope.launch {
            _insightsState.value = InsightsState.Loading

            val apiKey = "" // Replace with your key
            if (apiKey.isBlank()) {
                _insightsState.value = InsightsState.Error("API Key not set.")
                return@launch
            }

            val weeklyVolume = totalWeeklyVolume.first()
            val weeklyCals = weeklyCalories.first()
            val volByType = volumeByType.first()
            val calByType = caloriesByType.first()
            val dailyVols = dailyVolumeByType.first()

            val prompt = """
                Based on my weekly drinking data, provide a summary of my habits and offer actionable,
                healthy advice for improvement. Focus on the importance of enough water intake, 
                generally fewer calories, and how healthy different drinks are. 
                Keep the tone straightforward and concise.

                - My total consumption this week was: ${weeklyVolume.toInt()} ml.
                - My total calories from drinks this week were: ${weeklyCals.toInt()} kcal.

                Breakdown by Drink Type (Volume):
                ${volByType.map { "- ${it.key}: ${it.value.toInt()} ml" }.joinToString("\n")}

                Breakdown by Drink Type (Calories):
                ${calByType.map { "- ${it.key}: ${it.value.toInt()} kcal" }.joinToString("\n")}

                Daily Consumption Pattern (Volume):
                ${dailyVols.map { (day, types) -> "- $day: ${types.values.sum().toInt()} ml" }.joinToString("\n")}

                Please analyze this and give me some personalized insights using the following template:
                
                **Summary**: ...
                **Healthy Advice**: ...
            """.trimIndent()

            Log.d("ReportViewModel", "Generated Prompt:\n$prompt")

            val result = geminiService.getInsights(prompt, apiKey)

            result.onSuccess { insightText ->
                _insightsState.value = InsightsState.Success(insightText)
            }.onFailure { error ->
                _insightsState.value = InsightsState.Error(error.message ?: "An unknown error occurred.")
            }
        }
    }

    fun resetInsightsState() {
        _insightsState.value = InsightsState.Idle
    }

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

    private fun getUnifiedDrinkName(name: String,
                                    categoryTags: List<String>? = null,
                                    pnnsGroup: String? = null,
                                    foodGroups: List<String>? = null,
                                    ingredients: List<String>? = null
    ): String {
        val lower = name.lowercase(Locale.ROOT)

        val looksLikeWater = listOf(
            "water", "wasser", "mineral", "quelle", "sidi", "eau", "ain", "saiss", "naturelle"
        ).any { lower.contains(it) }

        val explicitlyNotWater = listOf("tea", "coffee", "kaffee", "latte").any { lower.contains(it) }

        val categoryMatch = categoryTags?.any {
            it.contains("water", ignoreCase = true) || it.contains("mineral", ignoreCase = true)
        } ?: false

        val pnnsMatch = pnnsGroup?.contains("waters", ignoreCase = true) == true

        val foodGroupMatch = foodGroups?.any {
            it.contains("water", ignoreCase = true)
        } ?: false

        val ingredientMatch = ingredients?.any {
            it.contains("water", ignoreCase = true)
        } ?: false

        return if ((looksLikeWater || categoryMatch || pnnsMatch || foodGroupMatch || ingredientMatch) && !explicitlyNotWater) {
            "Water"
        } else name
    }

    private fun getDrinkCategory(drink: DrinkProduct): String {
        val unifiedName = getUnifiedDrinkName(
            name = drink.getSensibleName(),
            categoryTags = drink.categoryTags,
            pnnsGroup = drink.pnnsGroup,
            foodGroups = drink.foodGroups,
            ingredients = drink.ingredients
        ).lowercase(Locale.ROOT)

        if (unifiedName == "water") return "Water"

        val categories = drink.categoryTags?.joinToString(",")?.lowercase(Locale.ROOT) ?: ""
        val ingredients = drink.ingredients?.joinToString(",")?.lowercase(Locale.ROOT) ?: ""
        val cal = drink.nutrients?.caloriesPer100g ?: 0.0
        val caf = drink.nutrients?.caffeinePer100g ?: 0.0
        val catTags = drink.categoryTags ?: emptyList()
        val pnns = drink.pnnsGroup?.lowercase(Locale.ROOT) ?: ""
        val foodGroups = drink.foodGroups ?: emptyList()

        return when {
            unifiedName.contains("coffee") || unifiedName.contains("kaffee") ||
                    unifiedName.contains("espresso") || unifiedName.contains("macchiato") || unifiedName.contains("latte") ||
                    catTags.any { it.contains("coffee") } -> "Coffee"

            unifiedName.contains("tea") || unifiedName.contains("tee") ||
                    categories.contains("tea") || catTags.any { it.contains("tea") } -> "Tea"

            unifiedName.contains("juice") || unifiedName.contains("cappy") ||
                    categories.contains("juice") || categories.contains("nectar") || categories.contains("smoothie") ||
                    catTags.any { it.contains("juice") || it.contains("nectar") } || pnns.contains("fruit juices") -> "Juice"

            categories.contains("soft drinks") || categories.contains("sugary drinks") ||
                    catTags.any { it.contains("soft-drinks") || it.contains("sugary") } ||
                    (cal > 20 && ingredients.contains("sugar")) -> "Sugary Drinks"

            unifiedName.contains("milch") || unifiedName.contains("milk") ||
                    categories.contains("milk") || ingredients.contains("milk") ||
                    foodGroups.any { it.contains("milk") } -> "Milk-Based Drinks"

            ingredients.contains("soja") || ingredients.contains("almond") || ingredients.contains("oat") ||
                    categories.contains("plant-based") || catTags.any { it.contains("plant-based") } -> "Plant-Based Drinks"

            categories.contains("energy drinks") || catTags.any { it.contains("energy-drinks") } || caf > 10.0 -> "Energy Drinks"

            categories.contains("sports drinks") || unifiedName.contains("isotonic") || catTags.any { it.contains("sports-drinks") } -> "Sports Drinks"

            categories.contains("alcohol") || catTags.any { it.contains("alcohol") } ||
                    unifiedName.contains("bier") || unifiedName.contains("wein") || unifiedName.contains("wine") -> "Alcoholic Drinks"

            else -> "Other"
        }
    }

    private fun aggregateVolumeByType(drinks: List<DrinkProduct>): Map<String, Double> {
        return drinks.groupBy { getDrinkCategory(it) }
            .mapValues { (_, drinkList) ->
                drinkList.sumOf { (it.consumedAmount ?: 100).toDouble() }
            }
    }

    private fun aggregateCaloriesByType(drinks: List<DrinkProduct>): Map<String, Double> {
        return drinks.groupBy { getDrinkCategory(it) }
            .mapValues { (_, drinkList) ->
                drinkList.sumOf { drink ->
                    drink.nutrients?.caloriesPer100g ?: 0.0
                }
            }
    }

    private fun aggregateDailyVolumeByType(drinks: List<DrinkProduct>): Map<String, Map<String, Double>> {
        return drinks.groupBy { toDateString(it.createdAt) }
            .mapValues { (_, dailyDrinks) ->
                dailyDrinks.groupBy { getDrinkCategory(it) }
                    .mapValues { (_, drinkList) ->
                        drinkList.sumOf { (it.consumedAmount ?: 100).toDouble() }
                    }
            }
    }

    private fun aggregateDailyCaloriesByType(drinks: List<DrinkProduct>): Map<String, Map<String, Double>> {
        return drinks.groupBy { toDateString(it.createdAt) }
            .mapValues { (_, dailyDrinks) ->
                dailyDrinks.groupBy { getDrinkCategory(it) }
                    .mapValues { (_, drinkList) ->
                        drinkList.sumOf { drink ->
                            drink.nutrients?.caloriesPer100g ?: 0.0
                        }
                    }
            }
    }

    private fun aggregateDailyWaterIntake(drinks: List<DrinkProduct>): Map<String, Double> {
        return drinks.filter { drink ->
            val name = drink.getSensibleName().lowercase(Locale.ROOT)

            val isNameLikeWater = name.contains("water") || name.contains("wasser")
            val isExplicitlyNotWater = name.contains("tea") || name.contains("tee") || name.contains("coffee") || name.contains("kaffee")

            val isZeroNutrient = drink.nutrients?.let {
                (it.caloriesPer100g ?: 0.0) == 0.0 &&
                        (it.sugarsPer100g ?: 0.0) == 0.0 &&
                        (it.saturatedFatPer100g ?: 0.0) == 0.0 &&
                        (it.caffeinePer100g ?: 0.0) == 0.0

            } ?: false

            (isNameLikeWater || isZeroNutrient) && !isExplicitlyNotWater }
            .groupBy { toDateString(it.createdAt) }
            .mapValues { (_, waterDrinks) ->
                waterDrinks.sumOf { (it.consumedAmount ?: 100).toDouble() }
            }
    }
}