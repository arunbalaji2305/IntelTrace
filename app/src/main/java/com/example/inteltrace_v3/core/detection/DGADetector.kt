package com.example.inteltrace_v3.core.detection

import kotlin.math.log2

object DGADetector {
    
    private const val DGA_ENTROPY_THRESHOLD = 3.5
    private const val MIN_DOMAIN_LENGTH = 8
    private const val MAX_CONSONANT_RATIO = 0.8
    private const val MIN_VOWEL_RATIO = 0.1
    
    private val KNOWN_TLD_WHITELIST = setOf(
        "com", "org", "net", "edu", "gov", "mil", "int",
        "io", "co", "ai", "app", "dev", "cloud", "tech"
    )
    
    private val VOWELS = setOf('a', 'e', 'i', 'o', 'u')
    
    data class DGAAnalysis(
        val isDGA: Boolean,
        val confidence: Double,
        val entropy: Double,
        val reason: String,
        val domainCharacteristics: Map<String, Any>
    )
    
    fun analyzeDomain(domain: String): DGAAnalysis {
        val cleanDomain = domain.lowercase().trim()
        val domainParts = cleanDomain.split(".")
        
        if (domainParts.size < 2) {
            return DGAAnalysis(
                isDGA = false,
                confidence = 0.0,
                entropy = 0.0,
                reason = "Invalid domain format",
                domainCharacteristics = emptyMap()
            )
        }
        
        val sld = domainParts[domainParts.lastIndex - 1]
        val tld = domainParts.last()
        
        if (sld.length < MIN_DOMAIN_LENGTH) {
            return DGAAnalysis(
                isDGA = false,
                confidence = 0.0,
                entropy = 0.0,
                reason = "Domain too short for DGA analysis",
                domainCharacteristics = emptyMap()
            )
        }
        
        val entropy = calculateShannonEntropy(sld)
        val consonantRatio = calculateConsonantRatio(sld)
        val vowelRatio = calculateVowelRatio(sld)
        val digitRatio = calculateDigitRatio(sld)
        val lengthScore = calculateLengthScore(sld)
        val nGramScore = calculateNGramScore(sld)
        
        var suspicionScore = 0.0
        val reasons = mutableListOf<String>()
        
        if (entropy > DGA_ENTROPY_THRESHOLD) {
            suspicionScore += 0.3
            reasons.add("High entropy: ${"%.2f".format(entropy)}")
        }
        
        if (consonantRatio > MAX_CONSONANT_RATIO) {
            suspicionScore += 0.2
            reasons.add("Unusual consonant ratio: ${"%.2f".format(consonantRatio)}")
        }
        
        if (vowelRatio < MIN_VOWEL_RATIO) {
            suspicionScore += 0.15
            reasons.add("Low vowel ratio: ${"%.2f".format(vowelRatio)}")
        }
        
        if (digitRatio > 0.3) {
            suspicionScore += 0.15
            reasons.add("High digit ratio: ${"%.2f".format(digitRatio)}")
        }
        
        if (lengthScore > 0.5) {
            suspicionScore += 0.1
            reasons.add("Unusual length pattern")
        }
        
        if (nGramScore > 0.6) {
            suspicionScore += 0.1
            reasons.add("Random character sequence detected")
        }
        
        if (!KNOWN_TLD_WHITELIST.contains(tld)) {
            suspicionScore += 0.05
        }
        
        val characteristics = mapOf(
            "entropy" to entropy,
            "consonantRatio" to consonantRatio,
            "vowelRatio" to vowelRatio,
            "digitRatio" to digitRatio,
            "length" to sld.length,
            "nGramScore" to nGramScore,
            "tld" to tld
        )
        
        val isDGA = suspicionScore > 0.5
        
        return DGAAnalysis(
            isDGA = isDGA,
            confidence = suspicionScore.coerceIn(0.0, 1.0),
            entropy = entropy,
            reason = if (isDGA) "DGA detected: ${reasons.joinToString(", ")}" else "Domain appears legitimate",
            domainCharacteristics = characteristics
        )
    }
    
    fun calculateShannonEntropy(text: String): Double {
        if (text.isEmpty()) return 0.0
        
        val charCounts = text.groupingBy { it }.eachCount()
        val length = text.length.toDouble()
        
        return charCounts.values.sumOf { count ->
            val probability = count / length
            -probability * log2(probability)
        }
    }
    
    private fun calculateConsonantRatio(text: String): Double {
        val letters = text.filter { it.isLetter() }
        if (letters.isEmpty()) return 0.0
        
        val consonants = letters.count { !VOWELS.contains(it) }
        return consonants.toDouble() / letters.length
    }
    
    private fun calculateVowelRatio(text: String): Double {
        val letters = text.filter { it.isLetter() }
        if (letters.isEmpty()) return 0.0
        
        val vowels = letters.count { VOWELS.contains(it) }
        return vowels.toDouble() / letters.length
    }
    
    private fun calculateDigitRatio(text: String): Double {
        if (text.isEmpty()) return 0.0
        return text.count { it.isDigit() }.toDouble() / text.length
    }
    
    private fun calculateLengthScore(text: String): Double {
        val length = text.length
        return when {
            length > 30 -> 0.8
            length > 20 -> 0.5
            length > 15 -> 0.3
            else -> 0.0
        }
    }
    
    private fun calculateNGramScore(text: String): Double {
        if (text.length < 3) return 0.0
        
        val trigrams = text.windowed(3, 1)
        val uniqueTrigrams = trigrams.toSet()
        
        val randomnessScore = uniqueTrigrams.size.toDouble() / trigrams.size
        
        val repeatedChars = text.windowed(2, 1).count { it[0] == it[1] }
        val repetitionPenalty = repeatedChars.toDouble() / (text.length - 1).coerceAtLeast(1)
        
        return (randomnessScore - repetitionPenalty).coerceIn(0.0, 1.0)
    }
}
