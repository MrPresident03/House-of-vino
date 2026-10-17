package com.houseofvino.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration

/** iOS driver (system SQLite) with foreign keys enabled. */
fun createNativeDriver(name: String = "houseofvino.db", inMemory: Boolean = false): SqlDriver =
    NativeSqliteDriver(
        schema = HouseOfVinoDatabase.Schema,
        name = name,
        onConfiguration = { config ->
            config.copy(
                inMemory = inMemory,
                extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true),
            )
        },
    )
