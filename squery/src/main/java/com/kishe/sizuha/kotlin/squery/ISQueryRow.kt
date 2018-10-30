package com.kishe.sizuha.kotlin.squery

import android.content.ContentValues
import android.database.Cursor

interface ISQueryRow {

    val tableName: String
    fun createEmptyRow(): ISQueryRow

    // Optional
    fun toValues(): ContentValues? = null
    fun loadFrom(cursor: Cursor) {}

}