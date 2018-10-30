package com.kishe.sizuha.kotlin.squery

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class SampleDB(context: Context, dbName: String, version: Int) : SQuery(context, dbName, version) {

    override fun onCreate(db: SQLiteDatabase?) {
        super.onCreate(db)
        // TODO ...
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // TODO ...
    }

}

class User : ISQueryRow {
    override val tableName = "user"
    override fun createEmptyRow() = User()

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

    fun getContext() = InstrumentationRegistry.getTargetContext()

    val userTable = User()

    fun openDB(): SQuery {
        return SampleDB(getContext(), "users.db", 1)
    }

    @Test
    @Throws(Exception::class)
    fun createAndDelete() {
        createTableAndInsert()

        val row = findInsertedRow()
        update(row)

        delete()
    }

    @Throws(Exception::class)
    fun createTableAndInsert() {
        openDB().use { db ->
            db.from(userTable).drop()
            db.createTable(User())

            val result = db.from(User().apply {
                lastName = "Numakura"
                firstName = "Manami"
                birth = 19780415
            }).insert()
            assertTrue(result)

            val count = db.from(userTable).count()
            assertEquals(count, 1)
        }
    }

    @Throws(Exception::class)
    fun findInsertedRow(): User {
        openDB().use { db ->
            return db.from(userTable).selectOne()!!
        }
    }

    @Throws(Exception::class)
    fun update(user: User) {
        openDB().use { db ->
            val result = db.from(userTable)
                    .values(ContentValues().apply {
                        put("idx", user.idx)
                        put("l_name", "沼倉")
                        put("f_name", "愛美")
                        put("birth", 19880415)
                    })
                    //.where("birth=?",19780415)
                    .update()
            assertEquals(result, 1)

            val count = db.from(userTable).where("birth=?", 19880415).count()
            assertEquals(count, 1)
        }
    }

    @Throws(Exception::class)
    fun delete() {
        openDB().use { db ->
            db.from(userTable).delete()

            val count = db.from(userTable).count()
            assertEquals(count, 0)
        }
    }


}