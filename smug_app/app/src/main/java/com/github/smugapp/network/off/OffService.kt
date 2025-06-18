package com.github.smugapp.network.off

import android.util.Log
import com.github.smugapp.data.SmugRepo
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
import kotlinx.serialization.json.Json

const val TAG = "OffService"

class OffService(private val repo: SmugRepo) {
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

    private suspend fun fetchProductByBarCode(code: String): DrinkProduct? {
        Log.d(TAG, "Fetching product by bar code: $code")
        val product = repo.getDrinkProductById(code)
        Log.d(TAG, "Fetched product: $product from database")
        if (product != null) {
            return product
        }

        try {
            val response = fetchHttpProductRequest(code)

            if (response.status.value != 200) {
                return null
            }

            val offResponse = response.body<OffResponse>()
            if (offResponse.status != 1) {
                return null
            }

            repo.insertDrinkProduct(offResponse.product)
            return offResponse.product
        } catch (e: Exception) {
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
            val products = offResponse.products.mapNotNull {
                Log.d(TAG, "Fetching product by bar code: ${it.code}")
                fetchProductByBarCode(it.code)
            }
            return products
        } catch (e: Exception) {
            return emptyList()
        }
    }

    suspend fun fetchProduct(searchTerm: InputParser): List<DrinkProduct> {
        Log.d(TAG, "Fetching product for search term: $searchTerm")

        return when (searchTerm) {
            is InputParser.BarCodeSearchTerm -> {
                val product = fetchProductByBarCode(searchTerm.value)
                if (product != null) {
                    listOf(product)
                } else {
                    emptyList()
                }
            }

            is InputParser.ProductSearchTerm -> fetchProductBySearchTerm(searchTerm.value)
            is InputParser.UnknownSearchTerm -> emptyList()
        }
    }


    fun close() {
        client.close()
    }
}