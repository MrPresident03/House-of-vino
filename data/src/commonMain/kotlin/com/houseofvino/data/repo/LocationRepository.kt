package com.houseofvino.data.repo

import com.houseofvino.core.model.Location
import com.houseofvino.core.model.LocationKind
import com.houseofvino.core.model.LocationTree
import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.db.WriteScope
import com.houseofvino.data.sync.SyncTables

data class LocationInput(
    val name: String,
    val kind: LocationKind,
    val parentId: String? = null,
    val gridRows: Int? = null,
    val gridCols: Int? = null,
    val sortOrder: Int = 0,
    val notes: String? = null,
)

data class LocationNode(val location: Location, val depth: Int, val bottlesHere: Int, val bottlesTotal: Int, val childCount: Int)

data class SlotView(
    val slotId: String?, val row: Int, val col: Int, val lotId: String?,
    /** The bottle in this position: its vintage, a short name, colour and label photo, for the grid. */
    val vintageId: String? = null, val label: String? = null, val color: String? = null, val photoPath: String? = null,
)

class LocationInUseException(message: String) : IllegalStateException(message)

class LocationRepository(private val database: LocalDatabase) {
    private val inventory get() = database.inventory

    fun all(collectionId: String): List<Location> =
        inventory.locationsForCollection(collectionId).executeAsList().map { it.toDomain() }

    fun byId(id: String): Location? = inventory.locationById(id).executeAsOneOrNull()?.toDomain()

    /** Depth-first tree with direct and rolled-up bottle counts. */
    fun tree(collectionId: String): List<LocationNode> {
        val locations = all(collectionId)
        val direct = inventory.bottlesPerLocation(collectionId).executeAsList()
            .associate { it.location_key to (it.bottles ?: 0L).toInt() }
        val children = locations.groupBy { it.parentId }
        val out = ArrayList<LocationNode>()
        fun total(id: String, guard: Int = 0): Int =
            (direct[id] ?: 0) + if (guard > 32) 0 else children[id].orEmpty().sumOf { total(it.id, guard + 1) }
        fun visit(parent: String?, depth: Int) {
            if (depth > 32) return
            for (loc in children[parent].orEmpty()) {
                out += LocationNode(loc, depth, direct[loc.id] ?: 0, total(loc.id), children[loc.id].orEmpty().size)
                visit(loc.id, depth + 1)
            }
        }
        visit(null, 0)
        // Orphans (parent deleted on another device) are shown at the top level rather than lost.
        val shown = out.map { it.location.id }.toSet()
        locations.filter { it.id !in shown }.forEach { out += LocationNode(it, 0, direct[it.id] ?: 0, total(it.id), 0) }
        return out
    }

    fun unassignedBottles(collectionId: String): Int =
        inventory.bottlesPerLocation(collectionId).executeAsList().firstOrNull { it.location_key == "" }?.bottles?.toInt() ?: 0

    fun pathName(id: String): String =
        LocationTree.path(id) { byId(it) }.joinToString(" › ") { it.name }

    fun create(collectionId: String, input: LocationInput): String = database.write {
        validate(input)
        input.parentId?.let { requireNotNull(byId(it)) { "Parent location not found" } }
        val id = database.newId()
        inventory.insertLocation(id, collectionId, input.parentId, input.name.trim(), input.kind.name, input.gridRows?.toLong(),
            input.gridCols?.toLong(), input.sortOrder.toLong(), input.notes.clean(), now, now)
        touched(SyncTables.LOCATION, id)
        id
    }

    fun update(id: String, input: LocationInput) = database.write {
        validate(input)
        val current = requireNotNull(byId(id)) { "Location not found" }
        if (LocationTree.wouldCreateCycle(id, input.parentId) { byId(it)?.parentId }) {
            throw IllegalArgumentException("A location can't be placed inside itself")
        }
        if (current.hasGrid && (input.gridRows != current.gridRows || input.gridCols != current.gridCols)) {
            val occupied = inventory.slotsForLocation(id).executeAsList()
                .filter { it.lot_id != null && (it.slot_row > (input.gridRows ?: 0) || it.slot_col > (input.gridCols ?: 0)) }
            if (occupied.isNotEmpty()) throw LocationInUseException("${occupied.size} bottle position(s) are outside the new grid")
        }
        inventory.updateLocation(input.parentId, input.name.trim(), input.kind.name, input.gridRows?.toLong(), input.gridCols?.toLong(),
            input.sortOrder.toLong(), input.notes.clean(), now, id)
        touched(SyncTables.LOCATION, id)
    }

    /** Deletes an empty location. Bottles must be moved first; child locations block deletion. */
    fun delete(id: String) = database.write {
        val children = inventory.childLocationCount(id).executeAsOne()
        if (children > 0) throw LocationInUseException("Move or delete the $children location(s) inside it first")
        val collection = requireNotNull(byId(id)).collectionId
        val bottles = inventory.bottlesPerLocation(collection).executeAsList().firstOrNull { it.location_key == id }?.bottles ?: 0L
        if (bottles > 0) throw LocationInUseException("Move the $bottles bottle(s) stored here first")
        inventory.softDeleteLocation(now, id)
        touched(SyncTables.LOCATION, id)
    }

    // Slots (optional individual bottle positions) ------------------------------------------------

    fun grid(locationId: String): List<SlotView> {
        val loc = byId(locationId) ?: return emptyList()
        if (!loc.hasGrid) return emptyList()
        val slots = inventory.slotsForLocation(locationId).executeAsList().associateBy { it.slot_row.toInt() to it.slot_col.toInt() }
        val bottles = bottlesForLots(slots.values.mapNotNull { it.lot_id }.distinct())
        return (1..loc.gridRows!!).flatMap { r ->
            (1..loc.gridCols!!).map { c ->
                slots[r to c].let { s ->
                    val b = s?.lot_id?.let(bottles::get)
                    SlotView(s?.id, r, c, s?.lot_id, b?.vintageId, b?.label, b?.color, b?.photoPath)
                }
            }
        }
    }

    private class SlotBottle(val vintageId: String, val label: String, val color: String?, val photoPath: String?)

    private fun bottlesForLots(lotIds: List<String>): Map<String, SlotBottle> {
        if (lotIds.isEmpty()) return emptyMap()
        val out = HashMap<String, SlotBottle>()
        for (chunk in lotIds.chunked(200)) {
            val sql = """
                SELECT l.id, v.id, w.producer, w.name, v.vintage_kind, v.year, w.color,
                       (SELECT p.local_path FROM photo p WHERE p.id = v.label_photo_id AND p.deleted_at IS NULL)
                FROM lot l JOIN vintage v ON v.id = l.vintage_id JOIN wine w ON w.id = v.wine_id
                WHERE l.id IN (${chunk.joinToString(",") { "?" }})
            """.trimIndent()
            database.driver.executeQuery(null, sql, { c ->
                while (c.next().value) {
                    val year = when (c.getString(4)) { "YEAR" -> c.getLong(5)?.toString(); "NV" -> "NV"; else -> null }
                    val label = listOfNotNull(year, c.getString(2), c.getString(3)).joinToString(" ")
                    out[c.getString(0)!!] = SlotBottle(c.getString(1)!!, label, c.getString(6), c.getString(7))
                }
                app.cash.sqldelight.db.QueryResult.Unit
            }, chunk.size) { chunk.forEachIndexed { i, id -> bindString(i, id) } }
        }
        return out
    }

    /**
     * Puts one bottle of [lotId] (stored at this location) into position (row, col). Refused when
     * the position is outside the grid, already taken, or when every bottle of the lot here is
     * already placed.
     */
    fun placeBottle(locationId: String, row: Int, col: Int, lotId: String) = database.write {
        val loc = requireNotNull(byId(locationId)) { "Location not found" }
        require(loc.hasGrid && row in 1..loc.gridRows!! && col in 1..loc.gridCols!!) { "Position is outside the grid" }
        val stock = inventory.stockBucket(lotId, locationId).executeAsOneOrNull()?.toInt() ?: 0
        val placed = inventory.slotCountForLotAt(lotId, locationId).executeAsOne().toInt()
        if (placed >= stock) throw LocationInUseException("All $stock bottle(s) of this purchase here already have a position")
        val existing = inventory.slotAt(locationId, row.toLong(), col.toLong()).executeAsOneOrNull()
        if (existing?.lot_id != null) throw LocationInUseException("That position is taken")
        upsertSlot(existing?.id, loc, row, col, lotId)
    }

    fun clearPosition(locationId: String, row: Int, col: Int) = database.write {
        val existing = inventory.slotAt(locationId, row.toLong(), col.toLong()).executeAsOneOrNull() ?: return@write
        inventory.setSlotLot(null, now, existing.id)
        touched(SyncTables.SLOT, existing.id)
    }

    private fun WriteScope.upsertSlot(slotId: String?, loc: Location, row: Int, col: Int, lotId: String?) {
        if (slotId != null) {
            inventory.setSlotLot(lotId, now, slotId)
            touched(SyncTables.SLOT, slotId)
        } else {
            val id = database.newId()
            inventory.insertSlot(id, loc.collectionId, loc.id, row.toLong(), col.toLong(), lotId, now, now)
            touched(SyncTables.SLOT, id)
        }
    }

    private fun validate(input: LocationInput) {
        require(input.name.isNotBlank()) { "Name is required" }
        require((input.gridRows == null) == (input.gridCols == null)) { "A grid needs both rows and columns" }
        require(input.gridRows == null || (input.gridRows in 1..200 && input.gridCols!! in 1..200)) { "Grids can be up to 200 × 200" }
    }
}
