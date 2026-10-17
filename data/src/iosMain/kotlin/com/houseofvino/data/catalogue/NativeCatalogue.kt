package com.houseofvino.data.catalogue

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.JournalMode
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

private object PackSchema : SqlSchema<QueryResult.Value<Unit>> {
    override val version = CataloguePack.FORMAT.toLong()
    override fun create(driver: SqlDriver) = QueryResult.Value(Unit)
    override fun migrate(driver: SqlDriver, oldVersion: Long, newVersion: Long, vararg callbacks: AfterVersion) = QueryResult.Value(Unit)
}

/**
 * Opens the catalogue bundled with the iOS app. The bundle is read-only, so the pack is copied
 * once per app version ([installTag]) into Caches (iOS may purge it; it is copied again).
 */
@OptIn(ExperimentalForeignApi::class)
fun openNativeCatalogue(installTag: String): WineCatalogue = runCatching {
    val source = NSBundle.mainBundle.pathForResource("lwin", "db") ?: return UnavailableCatalogue
    val dir = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String
    val fm = NSFileManager.defaultManager
    // Development builds keep one version number, so the pack's size also tags the copy.
    val size = (fm.attributesOfItemAtPath(source, null)?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
    val name = "catalogue-${CataloguePack.FORMAT}-$installTag-$size.db"
    val target = "$dir/$name"
    if (!fm.fileExistsAtPath(target)) {
        fm.contentsOfDirectoryAtPath(dir, null)?.filterIsInstance<String>()?.filter { it.startsWith("catalogue-") }?.forEach {
            fm.removeItemAtPath("$dir/$it", null)
        }
        check(fm.copyItemAtPath(source, target, null))
    }
    LwinCatalogue(NativeSqliteDriver(PackSchema, name, onConfiguration = { c ->
        c.copy(journalMode = JournalMode.DELETE, extendedConfig = c.extendedConfig.copy(basePath = dir))
    }))
}.getOrElse { UnavailableCatalogue }
