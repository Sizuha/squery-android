package com.kishe.sizuha.kotlin.squery

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.Closeable

open class SQuery(context: Context, dbName: String, version: Int)
    : SQLiteOpenHelper(context, dbName, null, version)
    , Closeable
{

    override fun onCreate(db: SQLiteDatabase?) {}

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    fun from(table: ISQueryRow, writable: Boolean = true): TableQuery {
        return TableQuery(if (writable) writableDatabase else readableDatabase, table)
    }

//    fun <T: ISQueryRow> from(factory: ()->T, writable: Boolean = true): TableQuery {
//        return TableQuery(if (writable) writableDatabase else readableDatabase, factory)
//    }

    fun createTable(table: ISQueryRow) {
        from(table).create()
    }

    fun rawQuery(sql: String, args: Array<out String>): Cursor? {
        return readableDatabase.rawQuery(sql, args)
    }

    fun execute(sql: String, args: Array<out String>) {
        writableDatabase.execSQL(sql, args)
    }

//    override fun close() {
//        super.close()
//    }

}