package com.houseofvino.data.repo

import com.houseofvino.core.inventory.MovementKind
import com.houseofvino.core.model.Ratings
import com.houseofvino.core.model.StructuredNotes
import com.houseofvino.core.model.Tasting
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.sync.SyncTables

data class TastingInput(
    val tastedOn: IsoDate,
    val rating: Int? = null,
    val structured: StructuredNotes = StructuredNotes(),
    val notes: String? = null,
    val occasion: String? = null,
    val foodPairing: String? = null,
    val companions: String? = null,
    val place: String? = null,
    val formatMl: Int? = null,
    val wouldBuyAgain: Boolean? = null,
)

/** A bottle opened from the cellar at the same time as the note is written. */
data class OpenedBottle(val lotId: String, val locationId: String?)

class JournalRepository(private val database: LocalDatabase, private val cellar: CellarRepository) {
    private val journal get() = database.journal

    fun tasting(id: String): Tasting? = journal.tastingById(id).executeAsOneOrNull()?.toDomain()

    fun forVintage(vintageId: String): List<Tasting> = journal.tastingsForVintage(vintageId).executeAsList().map { it.toDomain() }

    fun count(collectionId: String): Int = journal.journalCount(collectionId).executeAsOne().toInt()

    /**
     * Adds a tasting note. With [opened], the bottle is consumed in the same transaction and the
     * movement and the note reference each other; the note survives even if the stock record
     * is later reversed.
     */
    fun add(collectionId: String, vintageId: String, input: TastingInput, opened: OpenedBottle? = null): String = database.write {
        Ratings.validate(input.rating)
        requireNotNull(database.catalog.vintageById(vintageId).executeAsOneOrNull()) { "Wine not found" }
        val id = database.newId()
        val movementId = opened?.let {
            with(cellar) { departInScope(it.lotId, it.locationId, 1, MovementKind.CONSUME, null, null, null, null, id) }.id
        }
        val s = input.structured
        journal.insertTasting(
            id, collectionId, vintageId, input.tastedOn.toString(), input.rating?.toLong(), s.appearance?.name,
            s.noseIntensity?.name, s.sweetness?.name, s.acidity?.name, s.tannin?.name, s.alcohol?.name, s.body?.name,
            s.flavourIntensity?.name, s.finish?.name, s.aromas.joinToString(", ").ifEmpty { null }, input.notes.clean(),
            input.occasion.clean(), input.foodPairing.clean(), input.companions.clean(), input.place.clean(),
            (input.formatMl ?: opened?.let { o -> database.inventory.lotById(o.lotId).executeAsOne().format_ml.toInt() })?.toLong(),
            movementId, input.wouldBuyAgain?.let { if (it) 1L else 0L }, now, now,
        )
        touched(SyncTables.TASTING, id)
        id
    }

    fun update(id: String, input: TastingInput) = database.write {
        Ratings.validate(input.rating)
        requireNotNull(journal.tastingById(id).executeAsOneOrNull()) { "Tasting not found" }
        val s = input.structured
        journal.updateTasting(
            tastedOn = input.tastedOn.toString(), rating = input.rating?.toLong(), appearance = s.appearance?.name,
            noseIntensity = s.noseIntensity?.name, sweetness = s.sweetness?.name, acidity = s.acidity?.name,
            tannin = s.tannin?.name, alcohol = s.alcohol?.name, body = s.body?.name, flavourIntensity = s.flavourIntensity?.name,
            finish = s.finish?.name, aromas = s.aromas.joinToString(", ").ifEmpty { null }, notes = input.notes.clean(),
            occasion = input.occasion.clean(), foodPairing = input.foodPairing.clean(), companions = input.companions.clean(),
            place = input.place.clean(), formatMl = input.formatMl?.toLong(),
            wouldBuyAgain = input.wouldBuyAgain?.let { if (it) 1L else 0L }, updatedAt = now, id = id,
        )
        touched(SyncTables.TASTING, id)
    }

    /** Soft delete; the consumption movement (if any) is untouched. */
    fun delete(id: String) = database.write {
        journal.softDeleteTasting(now, id)
        touched(SyncTables.TASTING, id)
    }
}
