package com.example.inteltrace_v3.data.remote.api

import com.example.inteltrace_v3.data.remote.models.URLhausResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface URLhausService {
    
    @FormUrlEncoded
    @POST("api/")
    suspend fun queryURL(
        @Field("url") url: String
    ): URLhausResponse
    
    companion object {
        const val BASE_URL = "https://urlhaus-api.abuse.ch/"
    }
}
