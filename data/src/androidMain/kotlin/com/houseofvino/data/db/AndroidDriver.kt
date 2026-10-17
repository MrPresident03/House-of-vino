package com.houseofvino.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/** Android driver with foreign keys and write-ahead logging enabled. */
fun createAndroidDriver(context: Context, name: String = "houseofvino.db"): SqlDriver =
    AndroidSqliteDriver(
        schema = HouseOfVinoDatabase.Schema,
        context = context,
        name = name,
        callback = object : AndroidSqliteDriver.Callback(HouseOfVinoDatabase.Schema) {
            override fun onConfigure(db: SupportSQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
                db.enableWriteAheadLogging()
            }
        },
    )
