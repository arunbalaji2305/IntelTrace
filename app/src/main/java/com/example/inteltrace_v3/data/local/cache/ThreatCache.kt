package com.example.inteltrace_v3.data.local.cache

import com.example.inteltrace_v3.data.local.database.entities.ThreatEntity
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreatCache @Inject constructor() {
    
    private val cache = ConcurrentHashMap<String, CachedThreat>()
    private val cacheDurationMs = 24 * 60 * 60 * 1000L // 24 hours
    
    fun get(ipAddress: String): ThreatEntity? {
        val cached = cache[ipAddress]
        return if (cached != null && !cached.isExpired()) {
            cached.threat
        } else {
            cache.remove(ipAddress)
            null
        }
    }
    
    fun put(ipAddress: String, threat: ThreatEntity) {
        cache[ipAddress] = CachedThreat(threat, System.currentTimeMillis())
    }
    
    fun clear() {
        cache.clear()
    }
    
    fun removeExpired() {
        cache.entries.removeIf { it.value.isExpired() }
    }
    
    fun getSize(): Int = cache.size
    
    private data class CachedThreat(
        val threat: ThreatEntity,
        val cachedAt: Long
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - cachedAt > 24 * 60 * 60 * 1000L
        }
    }
}
