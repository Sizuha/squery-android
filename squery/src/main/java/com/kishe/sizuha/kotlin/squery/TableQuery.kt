package com.kishe.sizuha.kotlin.squery

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class TableQuery(db: SQLiteDatabase, tableName: String) : TableQueryBase(db, tableName) {

    fun reset(): TableQuery {
        super.clear()
        return this
    }

    fun create(tableDef: Any, ifNotExists: Boolean = true) {
        val sql = StringBuilder()
        sql.append("CREATE TABLE ")
        if (ifNotExists) sql.append("IF NOT EXISTS ")
        sql.append("`$tableName` (")

        var isFirstCol = true
        val pks = getKeyFields(tableDef)
        val isSinglePk = pks.size == 1

        for (member in tableDef::class.memberProperties) {
            member.findAnnotation<Column>()?.let { col ->
                if (isFirstCol) isFirstCol = false else sql.append(",")

                val pk = member.findAnnotation<PrimaryKey>()
                val isAutoInc = pk?.autoInc == true
                val dataType = if (isAutoInc) "INTEGER" else getDBColumnType(member)
                sql.append("${col.name} $dataType")

                if (isSinglePk && pk != null) {
                    sql.append(" PRIMARY KEY")
                    if (isAutoInc) {
                        sql.append(" AUTOINCREMENT")
                    }
//                    else {
//                        if (!col.orderByAsc) sql.append(" DESC")
//                    }
                }
                else {
                    if (col.notNull) {
                        sql.append(" NOT NULL")
                    }
                    if (col.unique) {
                        sql.append(" UNIQUE")
                    }
                }

                Unit
            }
        }

        if (pks.size > 1) {
            sql.append(", PRIMARY KEY(")

            isFirstCol = true
            for (p in pks) {
                if (isFirstCol) isFirstCol = false else sql.append(",")
                sql.append(p.name)
//                if (!p.orderByAsc) sql.append(" DESC")
            }

            sql.append(")")
        }

        sql.append(");")
        db.execSQL(sql.toString())
    }

    fun where(whereCond: String, vararg args: Any): TableQuery {
        setWhere(whereCond, *args)
        return this
    }

    fun whereWithList(whereCond: String, args: List<Any>): TableQuery {
        setWhereWithList(whereCond, args)
        return this
    }

    fun whereAnd(whereCond: String, vararg args: Any): TableQuery {
        pushWhereAnd(whereCond, args)
        return this
    }

    fun orderBy(field: String, asc: Boolean = true): TableQuery {
        pushOrderBy(field, asc)
        return this
    }

    fun orderByRaw(orderBy: String): TableQuery {
        setOrderBy(orderBy)
        return this
    }

    fun groupBy(field: String): TableQuery {
        pushGroupBy(field)
        return this
    }

    fun groupByRaw(groupBy: String): TableQuery {
        setGroupBy(groupBy)
        return this
    }

    fun having(havingStr: String, vararg args: String): TableQuery {
        setHaving(havingStr, *args)
        return this
    }

    fun distinct(enable: Boolean = true): TableQuery {
        setDistinct(enable)
        return this
    }

    fun limit(count: Int, offset: Int = 0): TableQuery {
        setLimit(count, offset)
        return this
    }

    fun columns(vararg cols: String): TableQuery {
        setColumns(*cols)
        return this
    }

    fun innerJoin(tables: List<String>, joinOn: String, args: List<String>): TableQuery {
        join(JoinType.INNER, tables, joinOn, args)
        return this
    }
    fun leftOuterJoin(tables: List<String>, joinOn: String, args: List<String>): TableQuery {
        join(JoinType.LEFT_OUTER, tables, joinOn, args)
        return this
    }
    fun crossJoin(tables: List<String>, joinOn: String, args: List<String>): TableQuery {
        join(JoinType.CROSS, tables, joinOn, args)
        return this
    }

    //------- SELECT -------//

    fun <R: ISQueryRow> select(factory: ()->R): MutableList<R> {
        return selectWithCursor { cur -> convertFromCursor(cur, factory) }
    }

    fun <R: ISQueryRow> selectOne(factory: ()->R): R? {
        setLimit(1)
        select(factory).forEach { row -> return row }
        return null
    }

    inline fun <R: ISQueryRow> selectForEach(noinline factory: ()->R, crossinline each: (row: R)->Unit) {
        selectForEachCursor {
            val row = convertFromCursor(it, factory)
            each(row)
        }
    }


    //------- VALUES -------//

    private fun setValues(row: Any) {
        sqlValues = if (row is ISQueryRow)
            row.toValues() ?: ContentValues()
        else ContentValues()

        for (member in row::class.memberProperties) {
            val column = member.findAnnotation<Column>() ?: continue
            if (column.exclude) continue

            val accessible = member.isAccessible
            member.isAccessible = true

            val rawValue = member.getter.call(row)
            if (rawValue == null) {
                sqlValues!!.putNull(column.name)
                continue
            }

            var pushed = false
            loop@ for (a in member.annotations) when (a) {
                is DateType -> {
                    val format = SimpleDateFormat(a.pattern, Locale.getDefault()).apply {
                        timeZone = if (a.timezone.isNotEmpty())
                            TimeZone.getTimeZone(a.timezone)
                        else
                            TimeZone.getDefault()
                    }

                    val valueStr: String = when (member.returnType) {
                        // Long --> Date (DB, TEXT)
                        Long::class.createType(),
                        Long::class.createType(nullable = true),
                        Long::class.javaPrimitiveType -> {
                            val t = rawValue as Long
                            format.format(Date().apply { time = t })
                        }

                        Date::class.createType(),
                        Date::class.createType(nullable = true),
                        Date::class.javaObjectType -> {
                            format.format(rawValue as Date)
                        }

                        Calendar::class.createType(),
                        Calendar::class.createType(nullable = true),
                        Calendar::class.javaObjectType -> {
                            val date = (rawValue as Calendar).time
                            format.format(date)
                        }

                        else -> rawValue.toString()
                    }
                    sqlValues!!.put(column.name, valueStr)
                    pushed = true
                    break@loop
                }

                is TimeStamp -> {
                    val stampVal: Long = when (member.returnType) {
                        // Date to Long (DB)
                        Date::class.createType(),
                        Date::class.createType(nullable = true),
                        Date::class.javaObjectType -> {
                            (rawValue as Date).time
                        }

                        Calendar::class.createType(),
                        Calendar::class.createType(nullable = true),
                        Calendar::class.javaObjectType -> {
                            (rawValue as Calendar).timeInMillis
                        }

                        else -> rawValue as Long
                    }
                    sqlValues!!.put(column.name, stampVal)
                    pushed = true
                    break@loop
                }
            }

            if (!pushed) when (member.returnType) {
                Boolean::class.createType(),
                Boolean::class.createType(nullable = true),
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaObjectType -> {
                    sqlValues!!.put(column.name, if (rawValue as Boolean) 1 else 0)
                }
                else -> sqlValues!!.put(column.name, rawValue.toString())
            }

            member.isAccessible = accessible
        }
    }

    //------- INSERT -------//
    fun insert(row: Any): Boolean {
        printLog("try: INSERT")
        setValues(row)

        row::class.memberProperties.firstOrNull {
            it.annotations.firstOrNull { a -> a is PrimaryKey && a.autoInc } != null
        }?.let {
            val column = it.annotations.firstOrNull { a -> a is Column } as? Column
            if (column != null) {
                val colName = column.name
                printLog("try remove key: $colName")
                sqlValues?.valueSet()?.removeAll { v ->
                    v.key == colName /*|| "$tableName.${v.key}" == "$tableName.$colName"*/
                }
            }
        }

        printValues()
        return db.insert(tableName, null, sqlValues) > 0
    }

    fun insert(values: ContentValues): Boolean {
        printLog("try: INSERT")
        sqlValues = values

        printValues()
        return db.insert(tableName, null, sqlValues) > 0
    }

    private fun printValues() {
        if (Config.enableDebugLog) {
            Log.d(LOG_TAG, "===values===")
            sqlValues?.valueSet()?.forEach {
                Log.d(LOG_TAG, "  ${it.key} => ${it.value}")
            }
            Log.d(LOG_TAG, "===END===")
        }
    }

    //------- UPDATE -------//
    fun update(row: Any, autoMakeWhere: Boolean = true): Int {
        printLog("try: UPDATE")

        val keys = getKeyFields(row).map { it.name }
        setValues(row)

        if (autoMakeWhere && sqlWhere.isEmpty()) {
            for (k in keys) {
                whereAnd("$k=?", sqlValues?.get(k).toString())
            }
        }

        sqlValues?.valueSet()?.removeAll {
            keys.contains(it.key) /*|| keys.contains("$tableName.${it.key}")*/
        }

        return update()
    }

    private fun update(): Int {
        val whereText = makeWhereText()
        if (Config.enableDebugLog) {
            printLog("where: $whereText")
            printValues()
        }
        return db.update(tableName, sqlValues, whereText, sqlWhereArgs.toTypedArray())
    }

    fun update(data: ContentValues): Int {
        setValues(data)
        return update()
    }

    //------- INSERT or UPDATE -------//
    fun insertOrUpdate(row: Any): Boolean {
        return insert(row) || update(row) > 0
    }

    //------- UPDATE or INSERT -------//
    fun updateOrInsert(row: Any): Boolean {
        return update(row) > 0 || insert(row)
    }

}