package com.github.smugapp.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.smugapp.data.DrinkRepo
import com.github.smugapp.model.DrinkProduct
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


class ReportViewModel(private val repo: DrinkRepo) : ViewModel() {

    private val _todayDrinks = repo.getTodayDrinkProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val todayDrinks: StateFlow<List<DrinkProduct>> = _todayDrinks

    val totalCalories: StateFlow<Double> = _todayDrinks
        .map { drinks ->
            drinks.sumOf { it.nutrients?.caloriesPer100g ?: 0.0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)
}


