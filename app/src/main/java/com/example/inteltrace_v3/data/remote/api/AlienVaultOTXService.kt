package com.example.inteltrace_v3.data.remote.api

import com.example.inteltrace_v3.data.remote.models.AlienVaultOTXResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface AlienVaultOTXService {
    
    @GET("indicators/IPv4/{ip}/general")
    suspend fun getIPReputation(
        @Path("ip") ip: String,
        @Header("X-OTX-API-KEY") apiKey: String
    ): Response<AlienVaultOTXResponse>
    
    @GET("indicators/IPv4/{ip}/malware")
    suspend fun getIPMalware(
        @Path("ip") ip: String,
        @Header("X-OTX-API-KEY") apiKey: String
    ): Response<AlienVaultOTXResponse.MalwareResponse>
    
    @GET("indicators/domain/{domain}/general")
    suspend fun getDomainReputation(
        @Path("domain") domain: String,
        @Header("X-OTX-API-KEY") apiKey: String
    ): Response<AlienVaultOTXResponse>
    
    companion object {
        const val BASE_URL = "https://otx.alienvault.com/api/v1/"
    }
}
