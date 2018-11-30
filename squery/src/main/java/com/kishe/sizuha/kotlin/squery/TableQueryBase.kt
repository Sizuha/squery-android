package com.kishe.sizuha.kotlin.squery

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase

abstract class TableQueryBase(protected val db: SQLiteDatabase, protected val tableName: String) {

    // FROM
    protected val sqlFrom: String = ""
    protected val sqlFromArgs = mutableListOf<String>()

    // WHERE
    protected var sqlWhere = StringBuilder()
    protected var sqlWhereArgs = mutableListOf<String>()

    protected var sqlValues: ContentValues? = null

    // for SELECT
    protected var sqlColumns = mutableListOf<String>()

    protected var sqlOrderByRaw: String? = null
    protected val sqlOrderBy = mutableListOf<Pair<String,Boolean>>()

    // GROUP BY
    protected var sqlGroupByRaw: String? = null
    protected val sqlGroupBy = mutableListOf<String>()
    protected var sqlHaving: String? = null
    protected var sqlHavingArgs = mutableListOf<String>()

    protected var sqlLimit = 0
    protected var sqlLimitOffset = 0

    protected var sqlDistinct = false

    // JOIN
    protected var sqlJoinType = JoinType.NONE
    protected var sqlJoinTables = mutableListOf<String>()
    protected var sqlJoinOn: String = ""
    protected var sqlJoinOnArgs = mutableListOf<String>()

    protected fun clear() {
        sqlWhere.clear()
        sqlWhereArgs.clear()

        sqlValues = null

        sqlColumns.clear()

        sqlOrderByRaw = null
        sqlOrderBy.clear()

        sqlGroupByRaw = null
        sqlGroupBy.clear()
        sqlHaving = null
        sqlHavingArgs.clear()

        sqlLimit = 0
        sqlLimitOffset = 0

        sqlDistinct = false

        sqlJoinType = JoinType.NONE
        sqlJoinTables.clear()
        sqlJoinOn = ""
        sqlJoinOnArgs.clear()
    }

    fun makeQueryString(forCount: Boolean, outSqlParams: MutableList<String>): String {
        val sql = StringBuilder()

        if (sqlGroupBy.isNotEmpty()) {
            sqlGroupByRaw = convertToCommaString(sqlGroupBy, true)
        }

        if (sqlOrderBy.isNotEmpty()) {
            sqlOrderByRaw = makeOrderByString()
        }

        sql.append("SELECT ")
        if (sqlDistinct) sql.append("DISTINCT ")

        if (forCount) {
            sql.append("count(*)")
        }
        else {
            if (sqlColumns.isEmpty()) {
                sql.append("*")
            }
            else {
                sql.append(convertToCommaString(sqlColumns, true))
            }
        }
        sql.append(" FROM `$tableName` ")

        // JOIN
        if (sqlJoinType != JoinType.NONE) {
            sql.append(when (sqlJoinType) {
                JoinType.LEFT_OUTER -> "LEFT OUTER JOIN "
                JoinType.CROSS -> "CROSS JOIN "
                else -> "INNER JOIN "
            })
            sql.append(convertToCommaString(sqlJoinTables, true))
            sql.append(" ON $sqlJoinOn ")

            outSqlParams.addAll(sqlJoinOnArgs)
        }

        // WHERE
        if (sqlWhere.isNotEmpty()) {
            sql.append(" WHERE ")
            sql.append(sqlWhere)

            outSqlParams.addAll(sqlWhereArgs)
        }

        // GROUP BY
        sqlGroupByRaw?.let { groupBy ->
            sql.append(" GROUP BY $groupBy ")

            // HAVING
            if (!sqlHaving.isNullOrEmpty()) {
                sql.append(" HAVING $sqlHaving ")
                outSqlParams.addAll(sqlHavingArgs)
            }
        }

        // ORDER BY
        sqlOrderByRaw?.let {
            sql.append(" ORDER BY $it ")
        }

        // LIMIT
        if (sqlLimit > 0) {
            sql.append(" LIMIT $sqlLimitOffset,$sqlLimit")
        }

        return sql.toString()
    }

    protected fun makeOrderByString(): String {
        val buff = StringBuilder()

        var isFirst = true
        for (op in sqlOrderBy) {
            if (isFirst) isFirst = false else buff.append(",")
            buff.append(op.first)
            if (!op.second) buff.append(" DESC")
        }

        return buff.toString()
    }

    protected fun join(joinType: JoinType, tables: List<String>, joinOn: String, args: List<String>) {
        sqlJoinType = joinType

        sqlJoinTables.clear()
        sqlJoinTables.addAll(tables)

        sqlJoinOn = joinOn

        sqlJoinOnArgs.clear()
        sqlJoinOnArgs.addAll(args)
    }

    protected fun makeWhereText(): String? =
            if (sqlWhere.isNotEmpty()) sqlWhere.toString() else null

    protected fun setColumns(vararg cols: String) {
        sqlColumns.clear()

        for (c in cols) {
            sqlColumns.add(c)
        }
    }

    protected fun setWhere(whereCond: String, vararg args: Any) {
        sqlWhere.clear()
        sqlWhere.append("($whereCond)")
        for (a in args) { sqlWhereArgs.add(a.toString()) }
    }

    protected fun setWhereWithList(whereCond: String, args: List<Any>) {
        sqlWhere.clear()
        sqlWhere.append("($whereCond)")
        for (a in args) { sqlWhereArgs.add(a.toString()) }
    }

    protected fun pushWhereAnd(whereCond: String, vararg args: Any) {
        if (sqlWhere.isNotEmpty()) sqlWhere.append(" AND ")
        sqlWhere.append("($whereCond)")
        for (a in args) { sqlWhereArgs.add(a.toString()) }
    }

    protected fun pushOrderBy(field: String, asc: Boolean = true) {
        sqlOrderBy.add(Pair(field, asc))
    }

    protected fun setOrderBy(orderBy: String) {
        sqlOrderBy.clear()
        sqlOrderByRaw = orderBy
    }

    protected fun pushGroupBy(field: String) {
        sqlGroupBy.add(field)
    }

    protected fun setGroupBy(groupBy: String) {
        sqlGroupBy.clear()
        sqlGroupByRaw = groupBy
    }

    protected fun setHaving(havingStr: String, vararg args: String) {
        sqlHaving = havingStr
        sqlHavingArgs.addAll(args)
    }

    protected fun setDistinct(enable: Boolean = true) {
        sqlDistinct = enable
    }

    protected fun setLimit(count: Int, offset: Int = 0) {
        sqlLimit = count
        sqlLimitOffset = offset
    }

    protected fun setValues(data: ContentValues) {
        sqlValues = data
    }


    //--- SELECT ---

    fun count(): Long {
        val sqlParams = mutableListOf<String>()
        val sql = makeQueryString(true, sqlParams)
        return DatabaseUtils.longForQuery(db, sql, sqlParams.toTypedArray())
    }

    fun selectAsCursor(vararg cols: String): Cursor? {
        if (cols.isNotEmpty()) setColumns(*cols)

        val sqlParams = mutableListOf<String>()
        val sql = makeQueryString(false, sqlParams)
        return db.rawQuery(sql, sqlParams.toTypedArray())
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

    fun <R> selectOneWithCursor(factory: (cursor: Cursor)->R): R? {
        setLimit(1)
        return selectWithCursor { cur -> factory(cur) }.firstOrNull()
    }

    fun selectForEachCursor(each: (cursor: Cursor)->Unit) {
        selectAsCursor().use { cur ->
            if (cur == null) return
            while (cur.moveToNext()) each(cur)
        }
    }


    //--- DELETE ---

    fun delete(): Int {
        return db.delete(tableName, makeWhereText(), sqlWhereArgs.toTypedArray())
    }

    //--- DROP ---
    fun drop() {
        val sql = StringBuilder()
        sql.append("DROP TABLE IF EXISTS `$tableName`;")
        db.execSQL(sql.toString())
    }


}