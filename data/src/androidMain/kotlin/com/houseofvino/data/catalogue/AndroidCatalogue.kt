package com.houseofvino.data.catalogue

import android.content.Context
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import java.io.File

/** The pack is prebuilt; opening it must never create or migrate anything. */
internal object PackSchema : SqlSchema<QueryResult.Value<Unit>> {
    override val version = CataloguePack.FORMAT.toLong()
    override fun create(driver: SqlDriver) = QueryResult.Value(Unit)
    override fun migrate(driver: SqlDriver, oldVersion: Long, newVersion: Long, vararg callbacks: AfterVersion) = QueryResult.Value(Unit)
}

/**
 * Opens the catalogue shipped in the APK's assets. SQLite can't read inside the APK, so the pack
 * is copied once per app install or update (tagged by [installTag]) into the databases folder.
 * Returns [UnavailableCatalogue] if the build has no pack.
 */
fun openAndroidCatalogue(context: Context, installTag: String): WineCatalogue = runCatching {
    val name = "catalogue-${CataloguePack.FORMAT}-$installTag.db"
    val target = context.getDatabasePath(name)
    if (!target.exists()) {
        target.parentFile?.mkdirs()
        target.parentFile?.listFiles { f -> f.name.startsWith("catalogue-") }?.forEach { it.delete() }
        val tmp = File(target.path + ".tmp")
        context.assets.open(CataloguePack.FILE_NAME).use { input -> tmp.outputStream().use { input.copyTo(it) } }
        check(tmp.renameTo(target))
    }
    LwinCatalogue(AndroidSqliteDriver(PackSchema, context, name))
}.getOrElse { UnavailableCatalogue }
