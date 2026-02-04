package com.example.inteltrace_v3.data.local.database.dao

import androidx.room.*
import com.example.inteltrace_v3.data.local.database.entities.AppInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appInfo: AppInfoEntity)
    
    @Query("SELECT * FROM app_info WHERE packageName = :packageName")
    suspend fun getAppInfo(packageName: String): AppInfoEntity?
    
    @Query("SELECT * FROM app_info ORDER BY totalConnections DESC")
    fun getAllApps(): Flow<List<AppInfoEntity>>
    
    @Query("SELECT * FROM app_info WHERE isWhitelisted = 1")
    fun getWhitelistedApps(): Flow<List<AppInfoEntity>>
    
    @Query("SELECT * FROM app_info WHERE isBlacklisted = 1")
    fun getBlacklistedApps(): Flow<List<AppInfoEntity>>
    
    @Query("SELECT * FROM app_info WHERE suspiciousConnections > 0 ORDER BY suspiciousConnections DESC")
    fun getSuspiciousApps(): Flow<List<AppInfoEntity>>
    
    @Update
    suspend fun update(appInfo: AppInfoEntity)
    
    @Query("DELETE FROM app_info WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
    
    @Query("DELETE FROM app_info")
    suspend fun deleteAll()
}
