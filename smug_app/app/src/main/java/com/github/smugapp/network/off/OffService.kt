package com.github.smugapp.network.off

import android.util.Log
import com.github.smugapp.model.DrinkProduct
import com.github.smugapp.model.OffResponse
import com.github.smugapp.model.OffSearchResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

const val TAG = "OffService"

sealed interface SearchState {
    data object Init : SearchState
    data object Loading : SearchState
    data class Success(val products: List<DrinkProduct>, val isComplete: Boolean = true) :
        SearchState

    data class Error(val reason: String) : SearchState
}

// REMOVED: The SmugRepo dependency is gone. The service no longer interacts with the database.
class OffService {
    private val urlOpenFoodFacts = "https://world.openfoodfacts.net/api/v2"
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
            })
        }
    }

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Init)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private suspend fun fetchHttpProductRequest(code: String) = client.request(urlOpenFoodFacts) {
        method = HttpMethod.Get
        url {
            appendPathSegments("product", code)
        }
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.UserAgent, "smug/1.0 (yann.pablo.bernard@stud.uni-hannover.de)")
        }
    }

    // REFACTORED: This function now ONLY fetches from the network.
    private suspend fun fetchProductByBarCode(code: String): DrinkProduct? {
        Log.d(TAG, "Fetching product by bar code from network: $code")

        // REMOVED: No more checking the local database here.
        // val product = repo.getDrinkProductById(code) ...

        try {
            val response = fetchHttpProductRequest(code)

            if (response.status.value != 200) {
                return null
            }

            val offResponse = response.body<OffResponse>()
            if (offResponse.status != 1) {
                return null
            }

            // REMOVED: The automatic insert is the key change. This line is gone.
            // repo.insertDrinkProduct(offResponse.product)

            return offResponse.product
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching product by barcode", e)
            return null
        }
    }

    private suspend fun fetchHttpSearchRequest(searchTerm: String) =
        client.request(urlOpenFoodFacts) {
            method = HttpMethod.Get
            url {
                appendPathSegments("search")
                parameters.append("categories_tags_en", searchTerm)
            }
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.UserAgent, "smug/1.0 (yann.pablo.bernard@stud.uni-hannover.de)")
            }
        }

    // This function is now "safe" because its helper `fetchProductByBarCode` no longer saves to the DB.
    private suspend fun fetchProductBySearchTerm(searchTerm: String): List<DrinkProduct> {
        Log.d(TAG, "Fetching product by search term: $searchTerm")
        if (searchTerm.isBlank()) {
            return emptyList()
        }

        try {
            val response = fetchHttpSearchRequest(searchTerm)
            if (response.status.value != 200) {
                return emptyList()
            }

            val offResponse = response.body<OffSearchResponse>()
            val products = mutableListOf<DrinkProduct>()

            _searchState.value = SearchState.Success(emptyList(), false)

            offResponse.products.forEach { productInfo ->
                Log.d(TAG, "Fetching full details for: ${productInfo.code}")
                // This call no longer has the side effect of saving to the database.
                val product = fetchProductByBarCode(productInfo.code)
                if (product != null) {
                    products.add(product)
                    _searchState.value = SearchState.Success(products.toList(), false)
                }
            }

            _searchState.value = SearchState.Success(products.toList(), true)
            return products
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching by search term", e)
            return emptyList()
        }
    }

    suspend fun searchProduct(searchTerm: InputParser) {
        Log.d(TAG, "Searching for product: $searchTerm")
        _searchState.value = SearchState.Loading

        try {
            val products = when (searchTerm) {
                is InputParser.BarCodeSearchTerm -> {
                    val product = fetchProductByBarCode(searchTerm.value)
                    if (product != null) listOf(product) else emptyList()
                }
                is InputParser.ProductSearchTerm -> {
                    // fetchProductBySearchTerm now updates the state internally.
                    fetchProductBySearchTerm(searchTerm.value)
                    return // Exit here as state is managed within the function.
                }
                is InputParser.UnknownSearchTerm -> emptyList()
            }

            _searchState.value = if (products.isNotEmpty()) {
                SearchState.Success(products)
            } else {
                // Changed to a success state with an empty list to avoid showing an error.
                SearchState.Success(emptyList(), isComplete = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching for product", e)
            _searchState.value = SearchState.Error("Search failed: ${e.message}")
        }
    }

    fun close() {
        client.close()
    }
}
