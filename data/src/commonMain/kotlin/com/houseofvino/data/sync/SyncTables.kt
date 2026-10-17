package com.houseofvino.data.sync

/** Column storage class, shared by SQLite (TEXT/INTEGER) and the JSON wire format. */
enum class ColType { TEXT, INTEGER }

/**
 * Description of one synchronised table. The column list is the wire contract with the server
 * (`supabase/migrations`) and is verified against the real SQLite schema by `SyncTablesSchemaTest`.
 */
data class SyncTable(
    val name: String,
    val columns: List<Pair<String, ColType>>,
    /** Append-only rows (the movement ledger): insert-if-absent, never updated or deleted. */
    val appendOnly: Boolean = false,
    /** Columns stored locally but never sent or overwritten by sync (device-specific). */
    val localOnlyColumns: Set<String> = emptySet(),
) {
    val columnNames: List<String> get() = columns.map { it.first }
    val syncedColumns: List<Pair<String, ColType>> get() = columns.filter { it.first !in localOnlyColumns }
    fun type(column: String): ColType? = columns.firstOrNull { it.first == column }?.second
}

object SyncTables {
    private val T = ColType.TEXT
    private val I = ColType.INTEGER

    private val SYNC_COLUMNS = listOf("created_at" to I, "updated_at" to I, "deleted_at" to I, "rev" to I)

    private fun table(name: String, vararg cols: Pair<String, ColType>, localOnly: Set<String> = emptySet()) =
        SyncTable(name, listOf("id" to T, "collection_id" to T) + cols.toList() + SYNC_COLUMNS, localOnlyColumns = localOnly)

    val WINE = table("wine",
        "producer" to T, "producer_sort" to T, "name" to T, "color" to T, "style" to T, "country" to T, "region" to T,
        "sub_region" to T, "appellation" to T, "classification" to T, "notes" to T)
    val WINE_GRAPE = table("wine_grape", "wine_id" to T, "grape" to T, "percent" to I, "position" to I)
    val VINTAGE = table("vintage",
        "wine_id" to T, "vintage_kind" to T, "year" to I, "drink_from" to I, "drink_until" to I,
        "drink_window_source" to T, "abv_bp" to I, "notes" to T, "label_photo_id" to T)
    val BARCODE = table("barcode", "code" to T, "symbology" to T, "wine_id" to T, "vintage_id" to T, "format_ml" to I)
    val TAG = table("tag", "name" to T, "color" to T)
    val VINTAGE_TAG = table("vintage_tag", "vintage_id" to T, "tag_id" to T)
    val LOCATION = table("location",
        "parent_id" to T, "name" to T, "kind" to T, "grid_rows" to I, "grid_cols" to I, "sort_order" to I, "notes" to T)
    val LOT = table("lot",
        "vintage_id" to T, "format_ml" to I, "acquired_on" to T, "source" to T, "order_ref" to T,
        "unit_price_minor" to I, "unit_price_currency" to T, "packaging" to T, "bottles_per_case" to I,
        "wishlist_item_id" to T, "notes" to T)
    val MOVEMENT = SyncTable(
        "movement",
        listOf(
            "id" to T, "collection_id" to T, "lot_id" to T, "location_id" to T, "delta" to I, "kind" to T,
            "occurred_at" to I, "transfer_group_id" to T, "reversal_of" to T, "amount_minor" to I,
            "amount_currency" to T, "counterparty" to T, "note" to T, "device_id" to T, "tasting_id" to T,
            "created_at" to I, "rev" to I,
        ),
        appendOnly = true,
    )
    val SLOT = table("slot", "location_id" to T, "slot_row" to I, "slot_col" to I, "lot_id" to T)
    val TASTING = table("tasting",
        "vintage_id" to T, "tasted_on" to T, "rating" to I, "appearance" to T, "nose_intensity" to T, "sweetness" to T,
        "acidity" to T, "tannin" to T, "alcohol" to T, "body" to T, "flavour_intensity" to T, "finish" to T,
        "aromas" to T, "notes" to T, "occasion" to T, "food_pairing" to T, "companions" to T, "place" to T,
        "format_ml" to I, "movement_id" to T, "would_buy_again" to I)
    val WISHLIST_ITEM = table("wishlist_item",
        "vintage_id" to T, "wine_id" to T, "producer" to T, "name" to T, "vintage_kind" to T, "year" to I,
        "format_ml" to I, "desired_qty" to I, "target_price_minor" to I, "target_price_currency" to T,
        "priority" to T, "status" to T, "notes" to T, "acquired_lot_id" to T)
    val MERCHANT_LINK = table("merchant_link",
        "wishlist_item_id" to T, "merchant" to T, "url" to T, "last_price_minor" to I, "last_price_currency" to T,
        "checked_on" to T)
    val VALUATION = table("valuation",
        "vintage_id" to T, "format_ml" to I, "amount_minor" to I, "amount_currency" to T, "source" to T,
        "as_of" to T, "note" to T)
    val PHOTO = table("photo",
        "owner_type" to T, "owner_id" to T, "local_path" to T, "remote_path" to T, "mime" to T, "width" to I,
        "height" to I, "bytes" to I, localOnly = setOf("local_path"))
    val EXCHANGE_RATE = table("exchange_rate",
        "base" to T, "quote" to T, "rate_e8" to I, "as_of" to T, "source" to T)
    val SAVED_SEARCH = table("saved_search",
        "name" to T, "scope" to T, "query_text" to T, "filters_json" to T, "sort" to T)

    /** Dependency order: parents before children, so pulls never violate foreign keys. */
    val ALL: List<SyncTable> = listOf(
        WINE, WINE_GRAPE, VINTAGE, BARCODE, TAG, VINTAGE_TAG, LOCATION, WISHLIST_ITEM, MERCHANT_LINK, LOT,
        MOVEMENT, SLOT, TASTING, VALUATION, PHOTO, EXCHANGE_RATE, SAVED_SEARCH,
    )

    private val byName = ALL.associateBy { it.name }

    fun byName(name: String): SyncTable = byName[name] ?: throw IllegalArgumentException("Not a synced table: $name")
}
