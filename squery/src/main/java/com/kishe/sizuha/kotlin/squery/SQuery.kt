package com.kishe.sizuha.kotlin.squery

import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.Closeable

open class SQuery(context: Context, dbName: String, version: Int)
    : SQLiteOpenHelper(context, dbName, null, version)
    , Closeable
{

    override fun onCreate(db: SQLiteDatabase?) {}

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    fun from(table: String, writable: Boolean = true): TableQuery {
        return TableQuery(if (writable) writableDatabase else readableDatabase, table)
    }

    fun createTable(tableName: String, tableDef: Any) {
        from(tableName).create(tableDef)
    }

    fun rawQuery(sql: String, args: Array<out String>): Cursor? {
        return readableDatabase.rawQuery(sql, args)
    }

    fun execute(sql: String, args: Array<out String>) {
        writableDatabase.execSQL(sql, args)
    }

    fun queryForLong(sql: String, args: Array<out String>): Long {
        DatabaseUtils.longForQuery(readableDatabase, sql, args)
        return 0
    }

}