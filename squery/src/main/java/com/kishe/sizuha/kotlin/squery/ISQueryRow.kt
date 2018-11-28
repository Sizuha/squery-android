package com.kishe.sizuha.kotlin.squery

import android.content.ContentValues
import android.database.Cursor

interface ISQueryRow {

    // Optional
    fun toValues(): ContentValues? = null
    fun loadFrom(cursor: Cursor) {}

}