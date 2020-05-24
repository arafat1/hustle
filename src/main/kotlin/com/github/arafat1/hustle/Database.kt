package com.github.arafat1.hustle

/**
 * Exposes methods for database specific activities
 */
interface Database {
    fun getJavaType(dbType: String): String
}