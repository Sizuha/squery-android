package com.kishe.sizuha.kotlin.squery

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class SampleDB(context: Context, dbName: String, version: Int) : SQuery(context, dbName, version) {

    override fun onCreate(db: SQLiteDatabase?) {
        super.onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

}

class User : ISQueryRow {
    companion object {
        val tableName = "user"
    }

    @Column("idx")
    @PrimaryKey(autoInc = true)
    var idx = 0
        private set

    @Column("l_name", notNull = true)
    var lastName = ""

    @Column("f_name", notNull = true)
    var firstName = ""

    @Column("birth", notNull = true)
    @DateType("yyyyMMdd")
    var birth: Int = 0

    @Column("email")
    var email: String? = null

    @Column("reg_date", notNull = true)
    @DateType
    var registerDate: Date = Date()
}

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private fun getContext() = InstrumentationRegistry.getTargetContext()

    private val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private fun openDB(): SQuery {
        return SampleDB(getContext(), "test-users.db", 1)
    }

    private fun releaseDB() {
        openDB().use {
            it.from(User.tableName).drop()
        }
    }

    //-------------- Basic Test

    @Test
    @Throws(Exception::class)
    fun createTableAndBasicTest() {
        createTableAndInsert()

        val row = findInsertedRow()
        update(row)

        deleteAll()
        releaseDB()
    }

    @Throws(Exception::class)
    private fun createTableAndInsert() {
        openDB().use { db ->
            db.from(User.tableName).drop()
            db.createTable(User.tableName, User())

            val result = db.from(User.tableName).insert(User().apply {
                lastName = "Numakura"
                firstName = "Manami"
                birth = 19780415
            })
            assertTrue(result)

            val count = db.from(User.tableName).count()
            assertEquals(count, 1)
        }
    }

    @Throws(Exception::class)
    private fun findInsertedRow(): User {
        openDB().use { db ->
            return db.from(User.tableName).selectOne { User() }!!
        }
    }

    @Throws(Exception::class)
    private fun update(user: User) {
        openDB().use { db ->
            val result = db.from(User.tableName)
                    .update(ContentValues().apply {
                        put("idx", user.idx)
                        put("l_name", "沼倉")
                        put("f_name", "愛美")
                        put("birth", 19880415)
                    })
            assertEquals(result, 1)

            val count = db.from(User.tableName).where("birth=?", 19880415).count()
            assertEquals(count, 1)
        }
    }

    @Throws(Exception::class)
    private fun deleteAll() {
        openDB().use { db ->
            db.from(User.tableName).delete()

            val count = db.from(User.tableName).count()
            assertEquals(count, 0)
        }
    }

    //-------------- INSERT / UPDATE Test

    class WrongUser : ISQueryRow {
        @Column("idx")
        var idx = 0

        @Column("l_name")
        var lastName = ""

        @Column("f_name")
        var firstName = ""
    }

    @Test
    @Throws(Exception::class)
    fun insertAndUpdateTest() {
        openDB().use { db ->
            db.from(User.tableName).create(User())
            var result = false

            //--- INSERT

            // row 1
            result = db.from(User.tableName).insert(User().apply {
                lastName = "Test"
                firstName = "User"
                birth = 19990101
                email = "test@test.com"
            })
            assertTrue(result)

            // row 2
            result = db.from(User.tableName).insert(User().apply {
                lastName = "Test"
                firstName = "User"
                birth = 19990101
                email = "test@test.com"
            })
            assertTrue(result)

            // row 3
            val regDateText = dateTimeFmt.format(dateTimeFmt.parse("1980-12-31 23:00:00"))
            result = db.from(User.tableName).insert(User().apply {
                lastName = "Test"
                firstName = "User"
                birth = 19990101
                email = "test@test.com"
                registerDate = dateTimeFmt.parse("1980-12-31 23:00:00")
            })
            assertTrue(result)
            val rowCount = db.from(User.tableName).count()

            // insert fail
            result = db.from(User.tableName).insert(WrongUser().apply {
                idx = 1
                lastName = "ERROR"
                firstName = "INSERT"
            })
            assertFalse(result)

            var resCount = db.from(User.tableName).where("l_name=?", "Test").count()
            assertEquals(resCount, rowCount)

            resCount = db.from(User.tableName).where("reg_date=?", regDateText).count()
            assertEquals(resCount, 1)


            //--- UPDATE
            resCount = db.from(User.tableName)
                    .where("l_name=?", "Test")
                    .update(ContentValues().apply {
                        put("l_name", "TEST")
                    }).toLong()
            assertEquals(resCount, rowCount)

            resCount = db.from(User.tableName)
                    .where("f_name=?", "User")
                    .update(ContentValues().apply {
                        put("f_name", "USER")
                    }).toLong()
            assertEquals(resCount, rowCount)

        }

        releaseDB()
    }

}