package com.kishe.sizuha.kotlin.squery

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Test

class SampleDB(context: Context, dbName: String, version: Int) : SQuery(context, dbName, version) {

    override fun onCreate(db: SQLiteDatabase?) {
        super.onCreate(db)

        // TODO ...
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // TODO ...
    }

}

//class SampleTable : ISQueryRow {
//
//    override val tableName: String
//        get() = "Table_Name"
//
//}

@RunWith(AndroidJUnit4::class)
class InstrumentationTest {

    @Test
    @Throws(Exception::class)
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals(appContext.packageName, "com.kishe.sizuha.kotlin.squery")
    }



}