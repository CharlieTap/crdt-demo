package com.contacts.crdtservice.plugins

import com.contacts.crdtservice.Database
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import io.ktor.server.application.*

fun Application.configureDatabase() : Database {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    Database.Schema.create(driver)
    return Database(driver)
}