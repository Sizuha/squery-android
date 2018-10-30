package com.kishe.sizuha.kotlin.squery

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class TableQuery<T: ISQueryRow>(private val db: SQLiteDatabase, private val table: T) {

    private val tableName: String = table.tableName

    // WHERE
    private var sqlWhere: String? = null
    private var sqlWhereArgs = mutableListOf<String>()

    private var sqlValues: ContentValues? = null

    // for SELECT
    private var sqlColumns = mutableListOf<String>()

    private var sqlOrderByRaw: String? = null
    private val sqlOrderBy = mutableListOf<Pair<String,Boolean>>()

    private var sqlGroupByRaw: String? = null
    private val sqlGroupBy = mutableListOf<String>()

    private var sqlHaving: String? = null

    private var sqlLimit = 0
    private var sqlLimitOffset = 0

    private var sqlDistinct = false

    // JOIN
    private var sqlJoinType = JoinType.NONE
    private var sqlJoinTables = mutableListOf<String>()
    private var sqlJoinOn: String = ""
    private var sqlJoinOnArgs = mutableListOf<String>()

    fun reset(): TableQuery<T> {
        sqlWhere = null
        sqlWhereArgs.clear()

        sqlValues = null

        sqlColumns.clear()

        sqlOrderByRaw = null
        sqlOrderBy.clear()

        sqlGroupByRaw = null
        sqlGroupBy.clear()

        sqlHaving = null

        sqlLimit = 0
        sqlLimitOffset = 0

        sqlDistinct = false

        sqlJoinType = JoinType.NONE
        sqlJoinTables.clear()
        sqlJoinOn = ""
        sqlJoinOnArgs.clear()

        return this
    }

    fun getKeyFieldNames(): List<String> {
        val result = mutableListOf<String>()
        for (member in table::class.memberProperties) {
            member.findAnnotation<PrimaryKey>()?.let { _ ->
                member.findAnnotation<Column>()?.let { column ->
                    result.add(column.name)
                }
            }
        }
        return result
    }

    fun getKeyFields(): List<Column> {
        val result = mutableListOf<Column>()

        table::class.memberProperties.filter {
            it.annotations.firstOrNull { a -> a is PrimaryKey } != null
        }.sortedBy {
            it.findAnnotation<PrimaryKey>()?.seq ?: 1
        }.forEach {
            it.findAnnotation<Column>()?.let { column -> result.add(column) }
        }

        return result
    }

    private fun getDBColumnType(member: KProperty1<out ISQueryRow, Any?>): String {
        return when (member.returnType) {
            Int::class.createType(),
            Int::class.javaPrimitiveType,
            Long::class.createType(),
            Long::class.javaPrimitiveType,
            Boolean::class.createType(),
            Boolean::class.javaPrimitiveType
            -> "INTEGER"

            Float::class.createType(),
            Float::class.javaPrimitiveType,
            Double::class.createType(),
            Double::class.javaPrimitiveType
            -> "REAL"

            ByteArray::class.createType()
            -> "BLOB"

            else -> "TEXT"
        }
    }

    fun create(ifNotExists: Boolean = true) {
        val sql = StringBuilder()
        sql.append("CREATE TABLE ")
        if (ifNotExists) sql.append("IF NOT EXISTS ")
        sql.append("`$tableName` (")

        var isFirstCol = true
        val pks = getKeyFields()
        val isSinglePk = pks.size == 1

        for (member in table::class.memberProperties) {
            member.findAnnotation<Column>()?.let { col ->
                if (isFirstCol) isFirstCol = false else sql.append(",")

                val isAutoInc = member.findAnnotation<PrimaryKey>()?.autoInc == true
                val dataType = if (isAutoInc) "INTEGER" else getDBColumnType(member)
                sql.append("${col.name} $dataType")

                if (isSinglePk) {
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

    fun drop() {
        val sql = StringBuilder()
        sql.append("DROP TABLE `$tableName`;")
        db.execSQL(sql.toString())
    }

    fun delete(): Int {
        return db.delete(tableName, sqlWhere, sqlWhereArgs.toTypedArray())
    }

    fun where(whereCond: String, vararg args: Any): TableQuery<T> {
        sqlWhere = whereCond
        for (a in args) { sqlWhereArgs.add(a.toString()) }
        return this
    }

    fun orderBy(field: String, asc: Boolean = true): TableQuery<T> {
        sqlOrderBy.add(Pair(field, asc))
        return this
    }

    fun orderByRaw(orderBy: String): TableQuery<T> {
        sqlOrderBy.clear()
        sqlOrderByRaw = orderBy
        return this
    }

    fun groupBy(field: String): TableQuery<T> {
        sqlGroupBy.add(field)
        return this
    }

    fun groupByRaw(groupBy: String): TableQuery<T> {
        sqlGroupBy.clear()
        sqlGroupByRaw = groupBy
        return this
    }

    fun having(havingStr: String) {
        sqlHaving = havingStr
    }

    fun distinct(enable: Boolean = true): TableQuery<T> {
        sqlDistinct = enable
        return this
    }

    fun limit(count: Int, offset: Int = 0): TableQuery<T> {
        sqlLimit = count
        sqlLimitOffset = offset
        return this
    }

    private fun getAllColumns(withoutKey: Boolean = false/*, withTableName: Boolean = false*/): MutableList<String> {
        val result = mutableListOf<String>()

        for (member in table::class.memberProperties) {
            member.findAnnotation<Column>()?.let { column ->
                val pk = member.findAnnotation<PrimaryKey>()
                if (!withoutKey || pk?.autoInc != true ) {
                    result.add(/*if (withTableName) "$tableName.${column.name}" else*/ column.name)
                }
            }
        }

        return result
    }

    fun columns(vararg cols: String/*, addTableName: Boolean = false*/): TableQuery<T> {
        sqlColumns.clear()

        for (c in cols) {
            sqlColumns.add(/*if (addTableName) "$tableName.$c" else*/ c)
        }

        return this
    }

    private fun convertToCommaString(source: Iterable<String>, withFieldQuote: Boolean = false): String {
        val buff = StringBuilder()

        var isFirst = true
        for (s in source) {
            if (isFirst) isFirst = false else buff.append(",")
            buff.append(if (withFieldQuote) "`$s`" else s)
        }

        return buff.toString()
    }

    private fun makeOrderByString(): String {
        val buff = StringBuilder()

        var isFirst = true
        for (op in sqlOrderBy) {
            if (isFirst) isFirst = false else buff.append(",")
            buff.append(op.first)
            if (!op.second) buff.append(" DESC")
        }

        return buff.toString()
    }

    fun innerJoin(tables: List<String>, joinOn: String, args: List<String>) {
        join(JoinType.INNER, tables, joinOn, args)
    }
    fun leftOuterJoin(tables: List<String>, joinOn: String, args: List<String>) {
        join(JoinType.LEFT_OUTER, tables, joinOn, args)
    }
    fun crossJoin(tables: List<String>, joinOn: String, args: List<String>) {
        join(JoinType.CROSS, tables, joinOn, args)
    }

    private fun join(joinType: JoinType, tables: List<String>, joinOn: String, args: List<String>) {
        sqlJoinType = joinType

        sqlJoinTables.clear()
        sqlJoinTables.addAll(tables)

        sqlJoinOn = joinOn

        sqlJoinOnArgs.clear()
        sqlJoinOnArgs.addAll(args)
    }

    fun selectAsCursor(vararg cols: String): Cursor? {
        if (cols.isNotEmpty()) columns(*cols)

        val cur: Cursor?
        var limitStr: String? = null

        if (sqlColumns.isEmpty()) {
            sqlColumns = getAllColumns()
        }

        if (sqlGroupBy.isNotEmpty()) {
            sqlGroupByRaw = convertToCommaString(sqlGroupBy, true)
        }

        if (sqlOrderBy.isNotEmpty()) {
            sqlOrderByRaw = makeOrderByString()
        }

        if (sqlLimit > 0) {
            limitStr = "$sqlLimitOffset,$sqlLimit"
        }

        if (sqlJoinType == JoinType.NONE) {
            cur = db.query(
                sqlDistinct,
                tableName,
                sqlColumns.toTypedArray(),
                sqlWhere,
                sqlWhereArgs.toTypedArray(),
                sqlGroupByRaw,
                sqlHaving,
                sqlOrderByRaw,
                limitStr
            )
        }
        else {
            val sql = StringBuilder()
            val sqlParams = mutableListOf<String>()

            sql.append("SELECT ")
            if (sqlDistinct) sql.append("DISTINCT ")

            sql.append(convertToCommaString(sqlColumns, true))
            sql.append(" FROM `$tableName` ")

            // JOIN
            sql.append(when (sqlJoinType) {
                JoinType.LEFT_OUTER -> "LEFT OUTER JOIN "
                JoinType.CROSS -> "CROSS JOIN "
                else -> "INNER JOIN "
            })
            sql.append(convertToCommaString(sqlJoinTables, true))
            sql.append(" ON $sqlJoinOn ")
            for (ja in sqlJoinOnArgs) sqlParams.add(ja)

            // WHERE
            sqlWhere?.let {
                sql.append(" WHERE $it ")
                for (wa in sqlWhereArgs) sqlParams.add(wa)
            }

            // GROUP BY
            sqlGroupByRaw?.let {
                sql.append(" GROUP BY $it ")
            }

            // HAVING
            sqlHaving?.let {
                sql.append(" HAVING $it ")
            }

            // ORDER BY
            sqlOrderByRaw?.let {
                sql.append(" ORDER BY $it ")
            }

            // LIMIT
            limitStr?.let {
                sql.append(" LIMIT $it ")
            }

            cur = db.rawQuery(sql.toString(), sqlParams.toTypedArray())
        }

        return cur
    }

    private fun findMemberInClass(tableObj: ISQueryRow, fieldName: String) =
        tableObj::class.memberProperties.firstOrNull {
            it.annotations.find { a -> a is Column && a.name == fieldName } != null
        }

    private fun setToProperty(colIdx: Int, tableObj: ISQueryRow, member: KMutableProperty<*>, cursor: Cursor) {
        val column = member.findAnnotation<Column>()
        if (column?.exclude == true) return

        var value: Any? = null

        val notNull = column?.notNull == true || !member.returnType.isMarkedNullable
        if (notNull || !cursor.isNull(colIdx)) {
            loop@ for (a in member.annotations) when (a) {
                is DateType -> {
                    val str = cursor.getString(colIdx)
                    val format = SimpleDateFormat(a.pattern, Locale.getDefault()).apply {
                        timeZone = if (a.timezone.isNotEmpty())
                            TimeZone.getTimeZone(a.timezone)
                        else
                            TimeZone.getDefault()
                    }
                    val date = format.parse(str)

                    value = when (member.returnType) {
                        Date::class.createType(),
                        Date::class.javaObjectType -> date

                        Calendar::class.createType(),
                        Calendar::class.javaObjectType -> {
                            Calendar.getInstance().apply { time = date }
                        }

                        // "yyyyMMdd" -> Int
                        Int::class.createType(),
                        Int::class.javaPrimitiveType,
                        Int::class.javaObjectType
                        -> str.toIntOrNull()

                        // "yyyyMMddhhmmss" -> Timestap(Long)
                        Long::class.createType(),
                        Long::class.javaPrimitiveType,
                        Long::class.javaObjectType
                        -> date.time

                        else -> str
                    }
                    break@loop
                }

                is TimeStamp -> {
                    val stamp = cursor.getLong(colIdx)

                    value = when (member.returnType) {
                        Date::class.createType(),
                        Date::class.javaObjectType -> Date().apply { time = stamp }

                        Calendar::class.createType(),
                        Calendar::class.javaObjectType -> {
                            Calendar.getInstance().apply {
                                timeZone = if (a.timezone.isNotEmpty())
                                    TimeZone.getTimeZone(a.timezone)
                                else
                                    TimeZone.getDefault()

                                timeInMillis = stamp
                            }
                        }

                        String::class.createType(),
                        String::class.javaObjectType
                        -> stamp.toString()

                        else -> stamp
                    }
                    break@loop
                }
            }

            value = value ?: when (member.returnType) {
                Int::class.createType(),
                Int::class.javaPrimitiveType,
                Int::class.javaObjectType
                -> cursor.getInt(colIdx)

                Boolean::class.createType(),
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaObjectType
                -> cursor.getInt(colIdx) != 0

                Long::class.createType(),
                Long::class.javaPrimitiveType,
                Long::class.javaObjectType
                -> cursor.getLong(colIdx)

                Float::class.createType(),
                Float::class.javaPrimitiveType,
                Float::class.javaObjectType
                -> cursor.getFloat(colIdx)

                Double::class.createType(),
                Double::class.javaPrimitiveType,
                Double::class.javaObjectType
                -> cursor.getDouble(colIdx)

                ByteArray::class.createType()
                -> return

                else -> cursor.getString(colIdx) ?: ""
            }
        }

        val accessible = member.isAccessible
        member.isAccessible = true
        member.setter.call(tableObj, value)
        member.isAccessible = accessible
    }

    fun select(): MutableList<T> {
        return selectWithCursor { cursor ->
            convertFromCursor(cursor) {
                table.createEmptyRow() as T
            }
        }
    }

    fun <R: ISQueryRow> select(factory: ()->R): MutableList<R> {
        return selectWithCursor { cur -> convertFromCursor(cur, factory) }
    }

    fun <R: ISQueryRow> convertFromCursor(cursor: Cursor, factory: ()->R): R {
        val row = factory()
        for (idx in 0 until cursor.columnCount) {
            val colName = cursor.getColumnName(idx)
            findMemberInClass(row, colName)?.let { member ->
                if (member is KMutableProperty<*>) {
                    setToProperty(idx, row, member, cursor)
                }
            }
        }
        row.loadFrom(cursor)

        return row
    }

    fun <R> selectWithCursor(factory: (cursor: Cursor)->R): MutableList<R> {
        val rows = mutableListOf<R>()

        return selectAsCursor().use { cur ->
            if (cur == null) return rows
            while (cur.moveToNext()) {
                val row = factory(cur)
                rows.add(row)
            }
            rows
        }
    }

    fun selectOne(): T? {
        return selectOne {
            table.createEmptyRow() as T
        }
    }

    fun <R: ISQueryRow> selectOne(factory: ()->R): R? {
        limit(1)
        select(factory).forEach { row -> return row }
        return null
    }

    fun <R> selectOneWithCursor(factory: (cursor: Cursor)->R): R? {
        limit(1)
        return selectWithCursor { cur -> factory(cur) }.firstOrNull()
    }

    fun selectForEachCursor(each: (cursor: Cursor)->Unit) {
        selectAsCursor().use { cur ->
            if (cur == null) return
            while (cur.moveToNext()) each(cur)
        }
    }

    fun selectForEach(each: (row: T)->Unit) {
        selectForEach({ table.createEmptyRow() as T }, each)
    }

    inline fun <R: ISQueryRow> selectForEach(noinline factory: ()->R, crossinline each: (row: R)->Unit) {
        selectForEachCursor {
            val row = convertFromCursor(it, factory)
            each(row)
        }
    }

    fun values(row: ISQueryRow): TableQuery<T> {
        sqlValues = row.toValues() ?: ContentValues()

        for (member in table::class.memberProperties) {
            val column = member.findAnnotation<Column>() ?: continue
            if (column.exclude) continue

            val accessible = member.isAccessible
            member.isAccessible = true

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
                        Long::class.javaPrimitiveType -> {
                            val t = member.getter.call(row) as Long
                            format.format(Date().apply { time = t })
                        }

                        Date::class.createType(),
                        Date::class.javaObjectType -> {
                            format.format(member.getter.call(row) as Date)
                        }

                        Calendar::class.createType(),
                        Calendar::class.javaObjectType -> {
                            val date = (member.getter.call(row) as Calendar).time
                            format.format(date)
                        }

                        else -> member.getter.call(row).toString()
                    }
                    sqlValues!!.put(column.name, valueStr)
                    pushed = true
                    break@loop
                }

                is TimeStamp -> {
                    val stampVal: Long = when (member.returnType) {
                        // Date to Long (DB)
                        Date::class.createType(),
                        Date::class.javaObjectType -> {
                            (member.getter.call(row) as Date).time
                        }

                        Calendar::class.createType(),
                        Calendar::class.javaObjectType -> {
                            (member.getter.call(row) as Calendar).timeInMillis
                        }

                        else -> member.getter.call(row) as Long
                    }
                    sqlValues!!.put(column.name, stampVal)
                    pushed = true
                    break@loop
                }
            }

            if (!pushed) when (member.returnType) {
                Boolean::class.createType(),
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaObjectType -> {
                    sqlValues!!.put(column.name, if (member.getter.call(row) as Boolean) 1 else 0)
                }
                else -> sqlValues!!.put(column.name, member.getter.call(row).toString())
            }

            member.isAccessible = accessible
        }

        return this
    }

    fun values(data: ContentValues): TableQuery<T> {
        sqlValues = data
        return this
    }

    // insert
    fun insert(): Boolean {
        printLog("try: INSERT")

        if (sqlValues == null) values(table)

        table::class.memberProperties.firstOrNull {
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

    private fun printValues() {
        if (Config.enableDebugLog) {
            Log.d(LOG_TAG, "===values===")
            sqlValues?.valueSet()?.forEach {
                Log.d(LOG_TAG, "  ${it.key} => ${it.value}")
            }
            Log.d(LOG_TAG, "===END===")
        }
    }

    fun insert(row: ISQueryRow): Boolean {
        values(row)
        return insert()
    }

    fun insert(data: ContentValues): Boolean {
        values(data)
        return insert()
    }

    // update
    fun update(autoMakeWhere: Boolean = true): Int {
        printLog("try: UPDATE")

        val keys = getKeyFields().map { it.name }
        if (sqlValues == null) values(table)

        if (autoMakeWhere && sqlWhere.isNullOrEmpty()) {
            sqlWhere = StringBuilder().apply {
                var isFirst = true
                for (k in keys) {
                    if (isFirst) isFirst = false else append(" AND ")

                    append("$k=?")
                    sqlWhereArgs.add(sqlValues?.get(k).toString())
                }
            }.toString()
        }

        sqlValues?.valueSet()?.removeAll {
            keys.contains(it.key) /*|| keys.contains("$tableName.${it.key}")*/
        }

        if (Config.enableDebugLog) {
            printLog("where: $sqlWhere")
            printValues()
        }

        return db.update(tableName, sqlValues, sqlWhere, sqlWhereArgs.toTypedArray())
    }

    fun update(row: ISQueryRow): Int {
        values(row)
        return update()
    }

    fun update(data: ContentValues): Int {
        values(data)
        return update()
    }

    //------- insert or update -------//
    fun insertOrUpdate(): Boolean {
        return insert() || update() > 0
    }

    fun insertOrUpdate(row: ISQueryRow): Boolean {
        values(row)
        return insertOrUpdate()
    }

    fun insertOrUpdate(data: ContentValues): Boolean {
        values(data)
        return insertOrUpdate()
    }

    //------- update or insert -------//
    private fun updateOrInsert(): Boolean {
        return if (sqlValues == null) updateOrInsert(table) else updateOrInsert(sqlValues!!)
    }

    fun updateOrInsert(row: ISQueryRow): Boolean {
        return update(row) > 0 || insert(row)
    }

    fun updateOrInsert(data: ContentValues): Boolean {
        val valuesForInsert = ContentValues().apply { putAll(sqlValues) }
        return update(data) > 0 || insert(valuesForInsert)
    }

}