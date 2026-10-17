package com.houseofvino.data

import app.cash.sqldelight.db.SqlDriver
import com.houseofvino.data.db.createNativeDriver
import kotlin.random.Random

actual fun createTestDriver(): SqlDriver =
    createNativeDriver(name = "test-${Random.nextLong().toULong()}.db", inMemory = true)
