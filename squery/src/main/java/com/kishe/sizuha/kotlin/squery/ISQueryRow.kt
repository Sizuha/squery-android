package com.kishe.sizuha.kotlin.squery

import android.content.ContentValues

interface ISQueryRow {

    val tableName: String
    fun toValues(): ContentValues? = null
    fun createEmptyRow(): ISQueryRow

}