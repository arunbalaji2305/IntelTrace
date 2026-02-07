package com.example.inteltrace_v3.core.utils

import java.security.MessageDigest
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

class BloomFilter(
    private val expectedElements: Int = 10000,
    private val falsePositiveRate: Double = 0.01
) {
    private val bitSetSize: Int
    private val hashFunctionCount: Int
    private val bitSet: BooleanArray
    
    init {
        bitSetSize = calculateOptimalBitSetSize(expectedElements, falsePositiveRate)
        hashFunctionCount = calculateOptimalHashFunctions(expectedElements, bitSetSize)
        bitSet = BooleanArray(bitSetSize)
    }
    
    fun add(element: String) {
        val hashes = getHashes(element)
        hashes.forEach { hash ->
            val index = (hash and Int.MAX_VALUE) % bitSetSize
            bitSet[index] = true
        }
    }
    
    fun addAll(elements: Collection<String>) {
        elements.forEach { add(it) }
    }
    
    fun mightContain(element: String): Boolean {
        val hashes = getHashes(element)
        return hashes.all { hash ->
            val index = (hash and Int.MAX_VALUE) % bitSetSize
            bitSet[index]
        }
    }
    
    fun clear() {
        bitSet.fill(false)
    }
    
    private fun getHashes(element: String): IntArray {
        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(element.toByteArray())
        
        val hash1 = bytesToInt(hashBytes, 0)
        val hash2 = bytesToInt(hashBytes, 4)
        
        return IntArray(hashFunctionCount) { i ->
            hash1 + i * hash2
        }
    }
    
    private fun bytesToInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }
    
    private fun calculateOptimalBitSetSize(n: Int, p: Double): Int {
        return ceil(-(n * ln(p)) / ln(2.0).pow(2)).toInt()
    }
    
    private fun calculateOptimalHashFunctions(n: Int, m: Int): Int {
        return ceil((m.toDouble() / n) * ln(2.0)).toInt().coerceAtLeast(1)
    }
    
    fun getSize(): Int = bitSetSize
    fun getHashFunctionCount(): Int = hashFunctionCount
    fun getEstimatedElementCount(): Int {
        val bitsSet = bitSet.count { it }
        if (bitsSet == 0) return 0
        val ratio = bitsSet.toDouble() / bitSetSize
        return (-bitSetSize / hashFunctionCount.toDouble() * ln(1 - ratio)).toInt()
    }
}
