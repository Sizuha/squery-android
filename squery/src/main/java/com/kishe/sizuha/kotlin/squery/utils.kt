package com.kishe.sizuha.kotlin.squery

import android.database.Cursor
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

fun findMemberInClass(tableObj: Any, fieldName: String) =
        tableObj::class.memberProperties.firstOrNull {
            it.annotations.find { a -> a is Column && a.name == fieldName } != null
        }

fun <R: Any> convertFromCursor(cursor: Cursor, factory: ()->R): R {
    val row = factory()
    for (idx in 0 until cursor.columnCount) {
        val colName = cursor.getColumnName(idx)
        findMemberInClass(row, colName)?.let { member ->
            if (member is KMutableProperty<*>) {
                setToProperty(idx, row, member, cursor)
            }
        }
    }

    if (row is ISQueryRow) {
        row.loadFrom(cursor)
    }
    return row
}

private fun setToProperty(colIdx: Int, tableObj: Any, member: KMutableProperty<*>, cursor: Cursor) {
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

fun getKeyFieldNames(rowDef: Any): List<String> {
    val result = mutableListOf<String>()
    for (member in rowDef::class.memberProperties) {
        member.findAnnotation<PrimaryKey>()?.let { _ ->
            member.findAnnotation<Column>()?.let { column ->
                result.add(column.name)
            }
        }
    }
    return result
}

/*fun getAllColumns(withoutKey: Boolean = false): MutableList<String> {
    val result = mutableListOf<String>()

    for (member in table::class.memberProperties) {
        member.findAnnotation<Column>()?.let { column ->
            val pk = member.findAnnotation<PrimaryKey>()
            if (!withoutKey || pk?.autoInc != true ) {
                result.add(column.name)
            }
        }
    }

    return result
}*/


fun getKeyFields(rowDef: Any): List<Column> {
    val result = mutableListOf<Column>()

    rowDef::class.memberProperties.filter {
        it.annotations.firstOrNull { a -> a is PrimaryKey } != null
    }.sortedBy {
        it.findAnnotation<PrimaryKey>()?.seq ?: 1
    }.forEach {
        it.findAnnotation<Column>()?.let { column -> result.add(column) }
    }

    return result
}

fun getDBColumnType(member: KProperty1<out Any, Any?>): String {
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

        String::class.createType(),
        String::class.javaPrimitiveType
        -> "TEXT"

        else -> "NONE"
    }
}

fun convertToCommaString(source: Iterable<String>, withFieldQuote: Boolean = false): String {
    val buff = StringBuilder()

    var isFirst = true
    for (s in source) {
        if (isFirst) isFirst = false else buff.append(",")
        buff.append(if (withFieldQuote) "`$s`" else s)
    }

    return buff.toString()
}
