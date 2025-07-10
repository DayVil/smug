package com.github.smugapp.network

import android.util.Log
// --- START OF CHANGE ---
// Import from the new KMP library's package
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
// --- END OF CHANGE ---
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiApiService {

    /**
     * Calls the Gemini API to get insights based on a prompt using the KMP library.
     *
     * @param prompt The detailed prompt containing user's drinking data.
     * @param apiKey Your Google AI Studio API key.
     * @return A Result containing the successful response text or an exception on failure.
     */
    suspend fun getInsights(prompt: String, apiKey: String): Result<String> {
        // Ensure this network call runs on a background thread
        return withContext(Dispatchers.IO) {
            try {
                // This constructor is the same, but it now refers to the class
                // from dev.shreyaspatil.generativeai.google
                val generativeModel = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = apiKey
                )

                val response = generativeModel.generateContent(prompt)

                // The response handling is also the same
                Result.success(response.text ?: "No response text found.")

            } catch (e: Exception) {
                Log.e("GeminiApiService", "API call failed", e)
                Result.failure(e)
            }
        }
    }
}