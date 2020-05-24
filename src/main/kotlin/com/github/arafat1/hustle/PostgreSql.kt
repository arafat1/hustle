package com.github.arafat1.hustle

class PostgreSql: Database {
    private val typeConversionTable = mapOf(
        "BOOL" to "Boolean",
        "VARCHAR" to "String",
        "CHAR" to "String",
        "TEXT" to "String",
        "NUMERIC" to "BigDecimal",
        "SMALLINT" to "Integer",
        "INT" to "Integer",
        "SERIAL" to "Integer",
        "BIGINT" to "Long",
        "DATE" to "Date",
        "TIMESTAMP" to "Timestamp"
    )

    override fun getJavaType(dbType: String): String {
        return typeConversionTable[dbType.toUpperCase()] ?: "String"
    }
}