package com.houseofvino.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

/**
 * JVM driver, used by tests and the desktop UI-test host. `path == null` gives a private
 * in-memory database.
 */
fun createJvmDriver(path: String? = null): SqlDriver {
    val url = if (path == null) JdbcSqliteDriver.IN_MEMORY else "jdbc:sqlite:$path"
    val driver = JdbcSqliteDriver(url, Properties().apply { put("foreign_keys", "true") })
    DatabaseFactory.migrate(driver)
    return driver
}
