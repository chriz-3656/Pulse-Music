package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow

object DebugLogger {
    val logs = MutableStateFlow<String>("--- DEBUG LOGS ---")

    fun log(msg: String) {
        android.util.Log.d("PulseDebug", msg)
        logs.value = logs.value + "\n> " + msg
    }
    
    fun clear() {
        logs.value = "--- DEBUG LOGS ---"
    }
}
