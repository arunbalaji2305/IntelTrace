package com.example.inteltrace_v3.core.di

import android.content.Context
import com.example.inteltrace_v3.data.local.database.IntelTraceDatabase
import com.example.inteltrace_v3.data.local.database.dao.*
import com.example.inteltrace_v3.data.remote.api.AbuseIPDBService
import com.example.inteltrace_v3.data.remote.api.URLhausService
import com.example.inteltrace_v3.data.remote.api.VirusTotalService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IntelTraceDatabase {
        return IntelTraceDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideConnectionDao(database: IntelTraceDatabase): ConnectionDao {
        return database.connectionDao()
    }
    
    @Provides
    @Singleton
    fun provideThreatDao(database: IntelTraceDatabase): ThreatDao {
        return database.threatDao()
    }
    
    @Provides
    @Singleton
    fun provideAppInfoDao(database: IntelTraceDatabase): AppInfoDao {
        return database.appInfoDao()
    }
    
    @Provides
    @Singleton
    fun provideAlertDao(database: IntelTraceDatabase): AlertDao {
        return database.alertDao()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAbuseIPDBService(okHttpClient: OkHttpClient): AbuseIPDBService {
        return Retrofit.Builder()
            .baseUrl(AbuseIPDBService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AbuseIPDBService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideVirusTotalService(okHttpClient: OkHttpClient): VirusTotalService {
        return Retrofit.Builder()
            .baseUrl(VirusTotalService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VirusTotalService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideURLhausService(okHttpClient: OkHttpClient): URLhausService {
        return Retrofit.Builder()
            .baseUrl(URLhausService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(URLhausService::class.java)
    }
}
