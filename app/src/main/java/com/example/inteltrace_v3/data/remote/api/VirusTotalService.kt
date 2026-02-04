package com.example.inteltrace_v3.data.remote.api

import com.example.inteltrace_v3.data.remote.models.VirusTotalIPResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface VirusTotalService {
    
    @GET("api/v3/ip_addresses/{ip}")
    suspend fun getIPReport(
        @Path("ip") ipAddress: String,
        @Header("x-apikey") apiKey: String
    ): VirusTotalIPResponse
    
    companion object {
        const val BASE_URL = "https://www.virustotal.com/"
    }
}
