package com.houseofvino.data

import app.cash.sqldelight.db.SqlDriver
import com.houseofvino.data.db.createJvmDriver

actual fun createTestDriver(): SqlDriver = createJvmDriver(null)
