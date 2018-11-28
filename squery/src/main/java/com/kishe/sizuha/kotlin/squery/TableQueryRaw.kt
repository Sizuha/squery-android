package com.kishe.sizuha.kotlin.squery

import android.database.sqlite.SQLiteDatabase

class TableQueryRaw(db: SQLiteDatabase, tableName: String) : TableQueryBase(db, tableName) {

    fun reset(): TableQueryRaw {
        super.clear()
        return this
    }

}