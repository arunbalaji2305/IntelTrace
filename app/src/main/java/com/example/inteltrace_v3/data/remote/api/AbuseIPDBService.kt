package com.example.inteltrace_v3.data.remote.api

import com.example.inteltrace_v3.data.remote.models.AbuseIPDBResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface AbuseIPDBService {
    
    @GET("api/v2/check")
    suspend fun checkIP(
        @Header("Key") apiKey: String,
        @Query("ipAddress") ipAddress: String,
        @Query("maxAgeInDays") maxAgeInDays: Int = 90,
        @Query("verbose") verbose: Boolean = false
    ): AbuseIPDBResponse
    
    companion object {
        const val BASE_URL = "https://api.abuseipdb.com/"
    }
}
