package com.kishe.sizuha.kotlin.squery

@Target(AnnotationTarget.CLASS)
annotation class Table(val name: String)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class Column(
        val name: String,
        val notNull: Boolean = false,
        val unique: Boolean = false,
        val manually: Boolean = false
)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class PrimaryKey(val seq: Int = 1, val autoInc: Boolean = false/*, val asc: Boolean = true*/)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class DateType(
        val pattern: String = "yyyy-MM-dd HH:mm:ss",
        val timezone: String = ""
)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class TimeStamp(
        val timezone: String = ""
)

enum class JoinType {
    NONE, INNER, LEFT_OUTER, CROSS
}
