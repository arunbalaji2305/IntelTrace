package com.example.inteltrace_v3.data.repository

import com.example.inteltrace_v3.data.local.database.dao.AlertDao
import com.example.inteltrace_v3.data.local.database.entities.AlertEntity
import com.example.inteltrace_v3.domain.models.ThreatLevel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    private val alertDao: AlertDao
) {
    
    suspend fun createAlert(
        threatLevel: ThreatLevel,
        title: String,
        message: String,
        ipAddress: String,
        packageName: String,
        appName: String,
        threatScore: Int
    ): Long {
        val alert = AlertEntity(
            timestamp = System.currentTimeMillis(),
            type = threatLevel.name,
            title = title,
            message = message,
            ipAddress = ipAddress,
            packageName = packageName,
            appName = appName,
            threatScore = threatScore
        )
        return alertDao.insert(alert)
    }
    
    fun getAllAlerts(): Flow<List<AlertEntity>> {
        return alertDao.getAllAlerts()
    }
    
    fun getUnreadAlerts(): Flow<List<AlertEntity>> {
        return alertDao.getUnreadAlerts()
    }
    
    fun getUnreadCount(): Flow<Int> {
        return alertDao.getUnreadCount()
    }
    
    fun getAlertsByType(type: String): Flow<List<AlertEntity>> {
        return alertDao.getAlertsByType(type)
    }
    
    suspend fun markAsRead(id: Long) {
        alertDao.markAsRead(id)
    }
    
    suspend fun dismiss(id: Long) {
        alertDao.dismiss(id)
    }
    
    suspend fun deleteOldAlerts(retentionDays: Int) {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        alertDao.deleteOldAlerts(cutoffTime)
    }
}
