package com.example.inteltrace_v3.data.remote.api

import com.example.inteltrace_v3.data.remote.models.PhishTankResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PhishTankService {
    
    @POST("checkurl/")
    suspend fun checkURL(
        @Body request: PhishTankRequest
    ): Response<PhishTankResponse>
    
    data class PhishTankRequest(
        val url: String,
        val format: String = "json",
        val app_key: String
    )
    
    companion object {
        const val BASE_URL = "https://checkurl.phishtank.com/"
    }
}
