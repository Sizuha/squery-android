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

    inline fun<reified T: Any> from(writable: Boolean = true): TableQuery {
        val tableName: String? = getTableName<T>()
        return from(tableName!!, writable)
    }

    inline fun<reified T: Any> createTable(db: SQLiteDatabase, tableDef: T, ifNotExists: Boolean = true) {
        val tableName: String? = getTableName<T>()
        TableQuery(db, tableName!!).create(tableDef, ifNotExists)
    }

    inline fun<reified T: Any> createTable(tableDef: T, ifNotExists: Boolean = true) {
        from<T>().create(tableDef, ifNotExists)
    }

    fun rawQuery(sql: String, args: Array<out String>): Cursor? {
        return readableDatabase.rawQuery(sql, args)
    }

    fun execute(sql: String, args: Array<out String>) {
        writableDatabase.execSQL(sql, args)
    }

    fun queryForLong(sql: String, args: Array<out String>): Long {
        return DatabaseUtils.longForQuery(readableDatabase, sql, args)
    }

}