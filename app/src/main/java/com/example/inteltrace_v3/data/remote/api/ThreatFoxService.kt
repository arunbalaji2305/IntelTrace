package com.example.inteltrace_v3.data.remote.api

import com.example.inteltrace_v3.data.remote.models.ThreatFoxResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ThreatFoxService {
    
    @POST("api/v1/")
    suspend fun searchIOC(
        @Body request: ThreatFoxRequest
    ): Response<ThreatFoxResponse>
    
    data class ThreatFoxRequest(
        val query: String,
        val search_term: String
    )
    
    companion object {
        const val BASE_URL = "https://threatfox-api.abuse.ch/"
        
        fun createIPSearchRequest(ip: String) = ThreatFoxRequest(
            query = "search_ioc",
            search_term = ip
        )
        
        fun createDomainSearchRequest(domain: String) = ThreatFoxRequest(
            query = "search_ioc",
            search_term = domain
        )
    }
}
