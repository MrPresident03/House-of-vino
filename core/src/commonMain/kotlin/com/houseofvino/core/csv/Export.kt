package com.houseofvino.core.csv

import com.houseofvino.core.model.BottleFormats
import com.houseofvino.core.money.Money

/**
 * Export schemas. Column names are stable (they are part of the import/export contract) and
 * the inventory export round-trips through [ColumnMapping.suggest].
 */
object Exports {

    data class InventoryRow(
        val producer: String, val wine: String?, val vintage: String, val color: String?, val style: String?,
        val country: String?, val region: String?, val subRegion: String?, val appellation: String?,
        val classification: String?, val grapes: String?, val formatMl: Int, val quantity: Int,
        val location: String?, val purchaseDate: String?, val unitPrice: Money?, val source: String?,
        val drinkFrom: Int?, val drinkUntil: Int?, val tags: String?, val notes: String?,
        val vintageId: String, val lotId: String,
    )

    val INVENTORY_HEADER = listOf(
        "Producer", "Wine", "Vintage", "Colour", "Style", "Country", "Region", "Subregion", "Appellation",
        "Classification", "Grapes", "Size", "Quantity", "Location", "Purchase date", "Price", "Currency", "Bought from",
        "Drink from", "Drink until", "Tags", "Notes", "Vintage ID", "Lot ID",
    )

    fun inventory(rows: Sequence<InventoryRow>): String = Csv.write(INVENTORY_HEADER, rows.map { r ->
        listOf(
            r.producer, r.wine, r.vintage, r.color, r.style, r.country, r.region, r.subRegion, r.appellation,
            r.classification, r.grapes, "${r.formatMl} ml", r.quantity.toString(), r.location, r.purchaseDate,
            r.unitPrice?.toPlainString(), r.unitPrice?.currency?.code, r.source, r.drinkFrom?.toString(),
            r.drinkUntil?.toString(), r.tags, r.notes, r.vintageId, r.lotId,
        )
    })

    data class TastingRow(
        val tastedOn: String, val producer: String, val wine: String?, val vintage: String, val rating: Int?,
        val notes: String?, val aromas: String?, val structured: String?, val occasion: String?,
        val foodPairing: String?, val companions: String?, val place: String?, val formatMl: Int?,
        val wouldBuyAgain: Boolean?, val tastingId: String, val vintageId: String,
    )

    val TASTING_HEADER = listOf(
        "Date", "Producer", "Wine", "Vintage", "Rating (100)", "Notes", "Aromas", "Structure", "Occasion",
        "Food pairing", "Companions", "Place", "Size", "Would buy again", "Tasting ID", "Vintage ID",
    )

    fun tastings(rows: Sequence<TastingRow>): String = Csv.write(TASTING_HEADER, rows.map { r ->
        listOf(
            r.tastedOn, r.producer, r.wine, r.vintage, r.rating?.toString(), r.notes, r.aromas, r.structured,
            r.occasion, r.foodPairing, r.companions, r.place, r.formatMl?.let { BottleFormats.formatVolume(it) },
            r.wouldBuyAgain?.let { if (it) "yes" else "no" }, r.tastingId, r.vintageId,
        )
    })

    /** Financial ledger: purchases, sales, gifts and valuations with their currencies and sources. */
    data class FinancialRow(
        val date: String, val type: String, val producer: String, val wine: String?, val vintage: String,
        val formatMl: Int, val quantity: Int, val unitAmount: Money?, val totalAmount: Money?,
        val counterparty: String?, val source: String?, val note: String?, val reference: String,
    )

    val FINANCIAL_HEADER = listOf(
        "Date", "Type", "Producer", "Wine", "Vintage", "Size", "Quantity", "Unit amount", "Total amount",
        "Currency", "Counterparty", "Source", "Note", "Reference",
    )

    fun financial(rows: Sequence<FinancialRow>): String = Csv.write(FINANCIAL_HEADER, rows.map { r ->
        listOf(
            r.date, r.type, r.producer, r.wine, r.vintage, "${r.formatMl} ml", r.quantity.toString(),
            r.unitAmount?.toPlainString(), r.totalAmount?.toPlainString(),
            (r.totalAmount ?: r.unitAmount)?.currency?.code, r.counterparty, r.source, r.note, r.reference,
        )
    })
}
