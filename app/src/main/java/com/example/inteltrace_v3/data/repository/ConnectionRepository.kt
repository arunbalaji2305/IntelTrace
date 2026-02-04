package com.example.inteltrace_v3.data.repository

import com.example.inteltrace_v3.data.local.database.dao.ConnectionDao
import com.example.inteltrace_v3.data.local.database.entities.ConnectionEntity
import com.example.inteltrace_v3.domain.models.NetworkConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepository @Inject constructor(
    private val connectionDao: ConnectionDao
) {
    
    suspend fun insertConnection(connection: ConnectionEntity): Long {
        return connectionDao.insert(connection)
    }
    
    fun getRecentConnections(limit: Int = 100): Flow<List<NetworkConnection>> {
        return connectionDao.getRecentConnections(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    fun getAllConnections(): Flow<List<NetworkConnection>> {
        return connectionDao.getAllConnections().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    fun getSuspiciousConnections(minScore: Int = 50): Flow<List<NetworkConnection>> {
        return connectionDao.getSuspiciousConnections(minScore).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    fun getConnectionsByPackage(packageName: String): Flow<List<NetworkConnection>> {
        return connectionDao.getConnectionsByPackage(packageName).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    suspend fun deleteOldConnections(cutoffTimestamp: Long) {
        connectionDao.deleteOldConnections(cutoffTimestamp)
    }
    
    suspend fun getConnectionCountSince(timestamp: Long): Int {
        return connectionDao.getConnectionCountSince(timestamp)
    }
}

private fun ConnectionEntity.toDomainModel() = NetworkConnection(
    id = id,
    timestamp = timestamp,
    sourceIp = sourceIp,
    destIp = destIp,
    sourcePort = sourcePort,
    destPort = destPort,
    protocol = protocol,
    packageName = packageName,
    appName = appName,
    bytesSent = bytesSent,
    bytesReceived = bytesReceived,
    threatScore = threatScore,
    isBlocked = isBlocked,
    country = country,
    city = city
)
