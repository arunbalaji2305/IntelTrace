package com.example.inteltrace_v3.data.local.database.dao

import androidx.room.*
import com.example.inteltrace_v3.data.local.database.entities.ConnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: ConnectionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(connections: List<ConnectionEntity>)
    
    @Query("SELECT * FROM connections ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentConnections(limit: Int = 100): Flow<List<ConnectionEntity>>
    
    @Query("SELECT * FROM connections ORDER BY timestamp DESC")
    fun getAllConnections(): Flow<List<ConnectionEntity>>
    
    @Query("SELECT * FROM connections WHERE threatScore > :minScore ORDER BY timestamp DESC")
    fun getSuspiciousConnections(minScore: Int = 50): Flow<List<ConnectionEntity>>
    
    @Query("SELECT * FROM connections WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getConnectionsByPackage(packageName: String): Flow<List<ConnectionEntity>>
    
    @Query("SELECT * FROM connections WHERE destIp = :ipAddress ORDER BY timestamp DESC")
    fun getConnectionsByIp(ipAddress: String): Flow<List<ConnectionEntity>>
    
    @Query("SELECT COUNT(*) FROM connections WHERE timestamp > :timestamp")
    suspend fun getConnectionCountSince(timestamp: Long): Int
    
    @Query("SELECT * FROM connections WHERE timestamp BETWEEN :startTime AND :endTime")
    fun getConnectionsInRange(startTime: Long, endTime: Long): Flow<List<ConnectionEntity>>
    
    @Query("DELETE FROM connections WHERE timestamp < :timestamp")
    suspend fun deleteOldConnections(timestamp: Long)
    
    @Query("DELETE FROM connections")
    suspend fun deleteAll()
    
    @Query("""
        SELECT packageName, appName, COUNT(*) as count, SUM(bytesSent) as totalSent, SUM(bytesReceived) as totalReceived
        FROM connections 
        WHERE timestamp > :since
        GROUP BY packageName 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getTopAppsByConnections(since: Long, limit: Int = 10): List<AppConnectionStats>
}

data class AppConnectionStats(
    val packageName: String,
    val appName: String,
    val count: Int,
    val totalSent: Long,
    val totalReceived: Long
)
