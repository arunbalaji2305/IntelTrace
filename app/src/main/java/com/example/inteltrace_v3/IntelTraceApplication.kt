package com.example.inteltrace_v3

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class IntelTraceApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
    }
}
