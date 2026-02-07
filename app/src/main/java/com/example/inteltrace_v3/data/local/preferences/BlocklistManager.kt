package com.example.inteltrace_v3.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocklistManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    data class ListEntry(
        val value: String,
        val type: EntryType,
        val addedAt: Long = System.currentTimeMillis(),
        val expiresAt: Long? = null,
        val reason: String? = null
    )
    
    enum class EntryType {
        IP_ADDRESS,
        DOMAIN,
        PACKAGE_NAME
    }
    
    fun addToBlocklist(
        value: String,
        type: EntryType,
        reason: String? = null,
        expiresAt: Long? = null
    ) {
        val blocklist = getBlocklist().toMutableList()
        
        blocklist.removeIf { it.value == value && it.type == type }
        
        blocklist.add(
            ListEntry(
                value = value,
                type = type,
                addedAt = System.currentTimeMillis(),
                expiresAt = expiresAt,
                reason = reason
            )
        )
        
        saveBlocklist(blocklist)
    }
    
    fun removeFromBlocklist(value: String, type: EntryType) {
        val blocklist = getBlocklist().toMutableList()
        blocklist.removeIf { it.value == value && it.type == type }
        saveBlocklist(blocklist)
    }
    
    fun isBlocked(value: String, type: EntryType): Boolean {
        val now = System.currentTimeMillis()
        return getBlocklist().any {
            it.value == value &&
            it.type == type &&
            (it.expiresAt == null || it.expiresAt > now)
        }
    }
    
    fun getBlocklist(): List<ListEntry> {
        val json = prefs.getString(KEY_BLOCKLIST, "[]") ?: "[]"
        val type = object : TypeToken<List<ListEntry>>() {}.type
        val list: List<ListEntry> = gson.fromJson(json, type)
        
        val now = System.currentTimeMillis()
        val validEntries = list.filter { it.expiresAt == null || it.expiresAt > now }
        
        if (validEntries.size != list.size) {
            saveBlocklist(validEntries)
        }
        
        return validEntries
    }
    
    fun addToAllowlist(
        value: String,
        type: EntryType,
        reason: String? = null,
        expiresAt: Long? = null
    ) {
        val allowlist = getAllowlist().toMutableList()
        
        allowlist.removeIf { it.value == value && it.type == type }
        
        allowlist.add(
            ListEntry(
                value = value,
                type = type,
                addedAt = System.currentTimeMillis(),
                expiresAt = expiresAt,
                reason = reason
            )
        )
        
        saveAllowlist(allowlist)
    }
    
    fun removeFromAllowlist(value: String, type: EntryType) {
        val allowlist = getAllowlist().toMutableList()
        allowlist.removeIf { it.value == value && it.type == type }
        saveAllowlist(allowlist)
    }
    
    fun isAllowed(value: String, type: EntryType): Boolean {
        val now = System.currentTimeMillis()
        return getAllowlist().any {
            it.value == value &&
            it.type == type &&
            (it.expiresAt == null || it.expiresAt > now)
        }
    }
    
    fun getAllowlist(): List<ListEntry> {
        val json = prefs.getString(KEY_ALLOWLIST, "[]") ?: "[]"
        val type = object : TypeToken<List<ListEntry>>() {}.type
        val list: List<ListEntry> = gson.fromJson(json, type)
        
        val now = System.currentTimeMillis()
        val validEntries = list.filter { it.expiresAt == null || it.expiresAt > now }
        
        if (validEntries.size != list.size) {
            saveAllowlist(validEntries)
        }
        
        return validEntries
    }
    
    fun importBlocklist(entries: List<ListEntry>) {
        val current = getBlocklist().toMutableList()
        entries.forEach { new ->
            current.removeIf { it.value == new.value && it.type == new.type }
            current.add(new)
        }
        saveBlocklist(current)
    }
    
    fun importAllowlist(entries: List<ListEntry>) {
        val current = getAllowlist().toMutableList()
        entries.forEach { new ->
            current.removeIf { it.value == new.value && it.type == new.type }
            current.add(new)
        }
        saveAllowlist(current)
    }
    
    fun exportBlocklist(): String {
        return gson.toJson(getBlocklist())
    }
    
    fun exportAllowlist(): String {
        return gson.toJson(getAllowlist())
    }
    
    fun clearBlocklist() {
        saveBlocklist(emptyList())
    }
    
    fun clearAllowlist() {
        saveAllowlist(emptyList())
    }
    
    private fun saveBlocklist(list: List<ListEntry>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_BLOCKLIST, json).apply()
    }
    
    private fun saveAllowlist(list: List<ListEntry>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_ALLOWLIST, json).apply()
    }
    
    companion object {
        private const val PREFS_NAME = "inteltrace_lists"
        private const val KEY_BLOCKLIST = "blocklist"
        private const val KEY_ALLOWLIST = "allowlist"
    }
}
