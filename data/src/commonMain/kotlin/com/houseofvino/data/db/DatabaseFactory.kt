package com.houseofvino.data.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.houseofvino.core.id.IdGenerator
import com.houseofvino.core.time.Clock
import com.houseofvino.data.repo.SettingsRepository

object DatabaseFactory {

    /**
     * Opens the database on [driver] (created by the platform with foreign keys on and the
     * schema migrated) and returns a ready [LocalDatabase]. A stable per-install device id is
     * created on first open; it labels ledger rows and is never derived from hardware.
     */
    fun open(driver: SqlDriver, clock: Clock = Clock.System, ids: IdGenerator = IdGenerator.Random): LocalDatabase {
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        val deviceId = deviceId(driver, ids)
        return LocalDatabase(driver, clock, ids, deviceId)
    }

    private fun deviceId(driver: SqlDriver, ids: IdGenerator): String {
        val queries = HouseOfVinoDatabase(driver).localQueries
        queries.getSetting(SettingsRepository.KEY_DEVICE_ID).executeAsOneOrNull()?.let { return it }
        val id = ids.newId()
        queries.putSetting(SettingsRepository.KEY_DEVICE_ID, id)
        return id
    }

    /** Creates or migrates a schema on a driver that doesn't do it itself (JDBC). */
    fun migrate(driver: SqlDriver) {
        val schema = HouseOfVinoDatabase.Schema
        val current = driver.executeQuery(null, "PRAGMA user_version", { c ->
            QueryResult.Value(if (c.next().value) c.getLong(0) ?: 0L else 0L)
        }, 0).value
        if (current == 0L) {
            schema.create(driver)
        } else if (current < schema.version) {
            schema.migrate(driver, current, schema.version)
        } else if (current > schema.version) {
            error("Database version $current is newer than this app (${schema.version}). Update the app.")
        }
        driver.execute(null, "PRAGMA user_version = ${schema.version}", 0)
    }
}
