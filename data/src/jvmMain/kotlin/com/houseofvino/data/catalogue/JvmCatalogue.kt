package com.houseofvino.data.catalogue

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/** Opens a pack file directly (tests and the desktop host). */
fun openJvmCatalogue(file: File): WineCatalogue =
    if (!file.exists()) UnavailableCatalogue else LwinCatalogue(JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}"))
