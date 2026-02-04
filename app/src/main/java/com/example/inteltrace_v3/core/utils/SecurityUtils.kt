package com.example.inteltrace_v3.core.utils

import kotlin.math.roundToInt

object SecurityUtils {
    
    fun calculateThreatLevel(score: Int): String {
        return when {
            score >= 80 -> "CRITICAL"
            score >= 60 -> "HIGH"
            score >= 40 -> "MEDIUM"
            score >= 20 -> "LOW"
            else -> "SAFE"
        }
    }
    
    fun getThreatLevelColor(score: Int): Long {
        return when {
            score >= 80 -> 0xFFF44336 // Red
            score >= 60 -> 0xFFFF9800 // Orange
            score >= 40 -> 0xFFFFC107 // Yellow
            score >= 20 -> 0xFF8BC34A // Light Green
            else -> 0xFF4CAF50 // Green
        }
    }
    
    fun sanitizeIP(ip: String): String {
        return ip.trim().replace(Regex("[^0-9.:]"), "")
    }
    
    fun isValidIPv4(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        
        return parts.all { part ->
            val num = part.toIntOrNull()
            num != null && num in 0..255
        }
    }
    
    fun isValidIPv6(ip: String): Boolean {
        val parts = ip.split(":")
        if (parts.size !in 3..8) return false
        
        return parts.all { part ->
            part.isEmpty() || part.matches(Regex("[0-9a-fA-F]{1,4}"))
        }
    }
    
    fun calculateRiskPercentage(score: Int): Int {
        return (score.toFloat() / 100 * 100).roundToInt()
    }
}
