package com.houseofvino.data.catalogue

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Builds the catalogue pack: `BuildCataloguePackKt <lwin.tsv.gz> <out lwin.db>`.
 * Run by the `:data:buildCataloguePack` Gradle task; uses the same text normaliser as the app.
 */
fun main(args: Array<String>) {
    require(args.size == 2) { "usage: <lwin.tsv.gz> <out.db>" }
    val src = File(args[0])
    val out = File(args[1])
    out.parentFile?.mkdirs()
    val tmp = File(out.path + ".tmp").apply { delete() }
    val driver = JdbcSqliteDriver("jdbc:sqlite:${tmp.absolutePath}")
    val n = GZIPInputStream(src.inputStream()).bufferedReader(Charsets.UTF_8).useLines { lines ->
        CataloguePack.build(driver, CataloguePack.parseTsv(lines), mapOf("source" to "LWIN (Liv-ex), CC BY 4.0", "file" to src.name))
    }
    driver.execute(null, "VACUUM", 0)
    driver.close()
    if (!tmp.renameTo(out)) { out.delete(); check(tmp.renameTo(out)) { "Couldn't write $out" } }
    println("Catalogue pack: $n wines, ${out.length() / 1024} KB -> $out")
}
