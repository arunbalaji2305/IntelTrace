package com.example.inteltrace_v3.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.inteltrace_v3.data.local.database.dao.*
import com.example.inteltrace_v3.data.local.database.entities.*

@Database(
    entities = [
        ConnectionEntity::class,
        ThreatEntity::class,
        AppInfoEntity::class,
        AlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class IntelTraceDatabase : RoomDatabase() {
    
    abstract fun connectionDao(): ConnectionDao
    abstract fun threatDao(): ThreatDao
    abstract fun appInfoDao(): AppInfoDao
    abstract fun alertDao(): AlertDao
    
    companion object {
        @Volatile
        private var INSTANCE: IntelTraceDatabase? = null
        
        fun getDatabase(context: Context): IntelTraceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IntelTraceDatabase::class.java,
                    "inteltrace_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
