package com.example.habittracker.util

import android.util.Log

object DebugLog {
    private const val PREFIX = "MyAppDebug"

    fun d(tag: String, message: String) {
        runCatching {
            Log.d("$PREFIX-$tag", message)
        }.getOrElse {
            println("$PREFIX-$tag: $message")
        }
    }
}
