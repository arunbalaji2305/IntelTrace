package com.example.inteltrace_v3.core.analysis

import android.util.Log
import com.example.inteltrace_v3.domain.models.NetworkPacket
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class FlowAnalyzer {
    private val activeFlows = ConcurrentHashMap<String, NetworkFlow>()
    private val completedFlows = mutableListOf<NetworkFlow>()
    private val mutex = Mutex()
    
    private val flowCallbacks = mutableListOf<(NetworkFlow) -> Unit>()
    
    suspend fun processPacket(packet: NetworkPacket): NetworkFlow? {
        val flowId = NetworkFlow.generateFlowId(
            packet.sourceIp,
            packet.destIp,
            packet.sourcePort,
            packet.destPort,
            packet.protocol
        )
        
        mutex.withLock {
            val flow = activeFlows.getOrPut(flowId) {
                NetworkFlow(
                    flowId = flowId,
                    sourceIp = packet.sourceIp,
                    destIp = packet.destIp,
                    sourcePort = packet.sourcePort,
                    destPort = packet.destPort,
                    protocol = packet.protocol,
                    startTime = packet.timestamp,
                    endTime = packet.timestamp
                )
            }
            
            flow.addPacket(
                timestamp = packet.timestamp,
                size = packet.packetSize,
                isOutgoing = true
            )
            
            return flow
        }
    }
    
    suspend fun cleanupInactiveFlows(currentTime: Long, timeoutMs: Long = 30000) {
        mutex.withLock {
            val inactiveFlows = activeFlows.filter { (_, flow) ->
                !flow.isActive(currentTime, timeoutMs)
            }
            
            inactiveFlows.forEach { (flowId, flow) ->
                activeFlows.remove(flowId)
                completedFlows.add(flow)
                
                flowCallbacks.forEach { callback ->
                    try {
                        callback(flow)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in flow callback: ${e.message}")
                    }
                }
            }
            
            if (completedFlows.size > MAX_COMPLETED_FLOWS) {
                completedFlows.removeAt(0)
            }
        }
    }
    
    fun onFlowCompleted(callback: (NetworkFlow) -> Unit) {
        flowCallbacks.add(callback)
    }
    
    fun getActiveFlowCount(): Int = activeFlows.size
    
    fun getCompletedFlowCount(): Int = completedFlows.size
    
    suspend fun getFlowStatistics(): FlowStatistics {
        mutex.withLock {
            val allFlows = activeFlows.values + completedFlows
            
            return FlowStatistics(
                totalFlows = allFlows.size,
                activeFlows = activeFlows.size,
                completedFlows = completedFlows.size,
                totalPackets = allFlows.sumOf { it.packetCount },
                totalBytes = allFlows.sumOf { it.bytesSent + it.bytesReceived },
                averageFlowDuration = allFlows.map { it.getDuration() }.average().toLong(),
                averagePacketsPerFlow = if (allFlows.isNotEmpty()) allFlows.sumOf { it.packetCount } / allFlows.size else 0
            )
        }
    }
    
    suspend fun clear() {
        mutex.withLock {
            activeFlows.clear()
            completedFlows.clear()
        }
    }
    
    data class FlowStatistics(
        val totalFlows: Int,
        val activeFlows: Int,
        val completedFlows: Int,
        val totalPackets: Int,
        val totalBytes: Long,
        val averageFlowDuration: Long,
        val averagePacketsPerFlow: Int
    )
    
    companion object {
        private const val TAG = "FlowAnalyzer"
        private const val MAX_COMPLETED_FLOWS = 1000
    }
}
