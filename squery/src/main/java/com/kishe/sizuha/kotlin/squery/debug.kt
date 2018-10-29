package com.kishe.sizuha.kotlin.squery

import android.util.Log

internal fun printLog(msg: String) {
    if (Config.enableDebugLog) {
        Log.d(LOG_TAG, msg)
    }
}