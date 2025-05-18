package com.github.smugapp.off

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
import java.nio.channels.UnresolvedAddressException

const val TAG = "OffService"

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

    private suspend fun fetchHttpRequest(code: String) = client.request(urlOpenFoodFacts) {
        method = HttpMethod.Get
        url {
            appendPathSegments("product", code)
        }
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.UserAgent, "smug/1.0 (yann.pablo.bernard@stud.uni-hannover.de)")
        }
    }

    suspend fun fetchProduct(code: String): Result<DrinkProduct> {
        try {
            val response = fetchHttpRequest(code)

            if (response.status.value != 200) {
                return Result.failure(Exception("Request failed with status code ${response.status.value}"))
            }

            val offResponse = response.body<OffResponse>()
            if (offResponse.status != 1) {
                return Result.failure(Exception("Request failed with status ${offResponse.statusVerbose}"))
            }

            return Result.success(offResponse.product)
        } catch (e: Exception) {
            if (e is UnresolvedAddressException) {
                return Result.failure(Exception("No internet connection"))
            }

            return Result.failure(e)
        }
    }

    fun close() {
        client.close()
    }
}