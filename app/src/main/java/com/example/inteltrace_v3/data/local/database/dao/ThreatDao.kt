package com.example.inteltrace_v3.data.local.database.dao

import androidx.room.*
import com.example.inteltrace_v3.data.local.database.entities.ThreatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreatDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(threat: ThreatEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(threats: List<ThreatEntity>)
    
    @Query("SELECT * FROM threats WHERE ipAddress = :ipAddress")
    suspend fun getThreatByIp(ipAddress: String): ThreatEntity?
    
    @Query("SELECT * FROM threats WHERE threatScore > :minScore ORDER BY threatScore DESC")
    fun getHighThreats(minScore: Int = 50): Flow<List<ThreatEntity>>
    
    @Query("SELECT * FROM threats WHERE isMalicious = 1 ORDER BY lastChecked DESC")
    fun getMaliciousIps(): Flow<List<ThreatEntity>>
    
    @Query("SELECT * FROM threats WHERE lastChecked < :timestamp")
    suspend fun getStaleThreats(timestamp: Long): List<ThreatEntity>
    
    @Query("DELETE FROM threats WHERE lastChecked < :timestamp")
    suspend fun deleteStaleThreats(timestamp: Long)
    
    @Query("DELETE FROM threats WHERE ipAddress = :ipAddress")
    suspend fun delete(ipAddress: String)
    
    @Query("DELETE FROM threats")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM threats WHERE isMalicious = 1")
    suspend fun getMaliciousCount(): Int
}
